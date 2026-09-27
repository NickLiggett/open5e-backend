package com.main.app.common.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Building blocks for list filters. Each returns {@code null} when its value is {@code null} (the filter wasn't
 * given), and {@link #and} drops those, so a filter record can list every condition unconditionally.
 */
public final class Specs {

    private Specs() {
    }

    public static Predicate and(CriteriaBuilder cb, Predicate... predicates) {
        return cb.and(Arrays.stream(predicates).filter(Objects::nonNull).toArray(Predicate[]::new));
    }

    /** Resources in any of the given documents. */
    public static Predicate inDocuments(Root<?> root, CriteriaBuilder cb, List<String> documents) {
        return documents == null || documents.isEmpty() ? null : root.get("documentKey").in(documents);
    }

    /** Case-insensitive substring match on {@code name}. */
    public static Predicate nameContains(Root<?> root, CriteriaBuilder cb, String name) {
        return contains(root, cb, "name", name);
    }

    /** Case-insensitive substring match; {@code %} and {@code _} in the value match literally. */
    public static Predicate contains(Root<?> root, CriteriaBuilder cb, String attribute, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String escaped = value.trim().toLowerCase()
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(root.get(attribute)), "%" + escaped + "%", '\\');
    }

    public static Predicate equal(Root<?> root, CriteriaBuilder cb, String attribute, Object value) {
        return value == null ? null : cb.equal(root.get(attribute), value);
    }

    public static <Y extends Comparable<? super Y>> Predicate atLeast(Root<?> root, CriteriaBuilder cb,
                                                                       String attribute, Y value) {
        return value == null ? null : cb.greaterThanOrEqualTo(root.get(attribute), value);
    }

    public static <Y extends Comparable<? super Y>> Predicate atMost(Root<?> root, CriteriaBuilder cb,
                                                                      String attribute, Y value) {
        return value == null ? null : cb.lessThanOrEqualTo(root.get(attribute), value);
    }

    /** {@code true}: the attribute is set; {@code false}: it's null. */
    public static Predicate present(Root<?> root, CriteriaBuilder cb, String attribute, Boolean present) {
        if (present == null) {
            return null;
        }
        return present ? cb.isNotNull(root.get(attribute)) : cb.isNull(root.get(attribute));
    }

    /** A JSON object column whose {@code key} is the value, e.g. a spell's school. */
    public static Predicate jsonKey(Root<?> root, CriteriaBuilder cb, String attribute, String key) {
        return key == null ? null
                : cb.equal(cb.function(JsonbFunctions.KEY, String.class, root.get(attribute)), key);
    }

    /** A JSON array of strings containing the value, e.g. a spell's damage types. */
    public static Predicate jsonArrayContains(Root<?> root, CriteriaBuilder cb, String attribute, String value) {
        return value == null ? null
                : cb.isTrue(cb.function(JsonbFunctions.ARRAY_CONTAINS, Boolean.class, root.get(attribute), cb.literal(value)));
    }

    /** A JSON array of {@code {key, ...}} objects containing one with this key, e.g. a spell's classes. */
    public static Predicate jsonArrayContainsKey(Root<?> root, CriteriaBuilder cb, String attribute, String key) {
        return key == null ? null
                : cb.isTrue(cb.function(JsonbFunctions.ARRAY_CONTAINS_KEY, Boolean.class, root.get(attribute), cb.literal(key)));
    }
}
