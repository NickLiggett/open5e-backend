package com.main.app.reference;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing languages. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record LanguageFilter(
        List<String> document,
        String name,
        Boolean isExotic,
        Boolean isSecret
) {

    public Specification<Language> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "isExotic", isExotic),
                Specs.equal(root, cb, "isSecret", isSecret));
    }
}
