package com.main.app.common.query;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypes;

/**
 * SQL functions for filtering on {@code jsonb} columns in criteria queries. Registered with Hibernate through
 * {@code META-INF/services/org.hibernate.boot.model.FunctionContributor}.
 */
public class JsonbFunctions implements FunctionContributor {

    /** {@code jsonb_key(column)}: the {@code key} field of a JSON object, e.g. a spell's school. */
    public static final String KEY = "jsonb_key";

    /** {@code jsonb_array_contains(column, value)}: whether a JSON array of strings contains the value. */
    public static final String ARRAY_CONTAINS = "jsonb_array_contains";

    /**
     * {@code jsonb_array_contains_key(column, key)}: whether a JSON array of objects contains one with this
     * {@code key}, e.g. a spell's classes. Uses {@code @>}, so GIN indexes on the column apply.
     */
    public static final String ARRAY_CONTAINS_KEY = "jsonb_array_contains_key";

    @Override
    public void contributeFunctions(FunctionContributions functions) {
        var types = functions.getTypeConfiguration().getBasicTypeRegistry();
        BasicType<String> string = types.resolve(StandardBasicTypes.STRING);
        BasicType<Boolean> bool = types.resolve(StandardBasicTypes.BOOLEAN);
        var registry = functions.getFunctionRegistry();

        registry.registerPattern(KEY, "(?1->>'key')", string);
        registry.registerPattern(ARRAY_CONTAINS, "(?1 @> jsonb_build_array(cast(?2 as text)))", bool);
        registry.registerPattern(ARRAY_CONTAINS_KEY,
                "(?1 @> jsonb_build_array(jsonb_build_object('key', cast(?2 as text))))", bool);
    }
}
