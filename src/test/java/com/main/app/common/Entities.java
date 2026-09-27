package com.main.app.common;

import com.main.app.document.Document;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Reflection helpers for tests that check every entity the same way. */
public final class Entities {

    private Entities() {
    }

    /** All entity classes, sorted by name. */
    public static List<Class<?>> all(EntityManager em) {
        return em.getMetamodel().getEntities().stream()
                .<Class<?>>map(EntityType::getJavaType)
                .sorted(Comparator.comparing(Class::getSimpleName))
                .toList();
    }

    public static String table(Class<?> entity) {
        return entity.getAnnotation(Table.class).name();
    }

    /** SQL restricting a table to rows an anonymous user can see (default content). */
    public static String anonymouslyVisible(Class<?> entity) {
        if (OwnedResource.class.isAssignableFrom(entity)) {
            return "document_key in (select key from open5e.documents where owner_id is null)";
        }
        return entity == Document.class ? "owner_id is null" : "true";
    }

    /** Fields of the class and its superclasses, superclass fields first. */
    public static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            fields.addAll(0, List.of(c.getDeclaredFields()));
        }
        fields.removeIf(f -> java.lang.reflect.Modifier.isStatic(f.getModifiers()));
        fields.forEach(f -> f.setAccessible(true));
        return fields;
    }

    public static Optional<Field> field(Class<?> type, String name) {
        return fields(type).stream().filter(f -> f.getName().equals(name)).findFirst();
    }

    /** JSON column name → field, for {@code @JdbcTypeCode(SqlTypes.JSON)} fields. */
    public static Map<String, Field> jsonColumns(Class<?> entity) {
        Map<String, Field> columns = new LinkedHashMap<>();
        for (Field field : fields(entity)) {
            JdbcTypeCode type = field.getAnnotation(JdbcTypeCode.class);
            if (type != null && type.value() == SqlTypes.JSON) {
                columns.put(field.getAnnotation(Column.class).name(), field);
            }
        }
        return columns;
    }

    public static Object get(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
