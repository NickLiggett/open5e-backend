package com.main.app.common.json;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Jackson settings for JSON stored in the database, which uses the Open5e snake_case keys. API responses use Spring's
 * own camelCase mapper; this one is only for {@code jsonb} columns, so it is deliberately not a Spring bean.
 */
public final class DatabaseJson {

    public static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    private DatabaseJson() {
    }
}
