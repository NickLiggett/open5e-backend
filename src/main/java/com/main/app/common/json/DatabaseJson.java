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
            .propertyNamingStrategy(new SnakeCaseWithNumbers())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    private DatabaseJson() {
    }

    /**
     * snake_case that also separates numbers, as the Open5e data does: {@code hitPointsAt1stLevel} becomes
     * {@code hit_points_at_1st_level} rather than {@code hit_points_at1st_level}.
     */
    static final class SnakeCaseWithNumbers extends PropertyNamingStrategies.SnakeCaseStrategy {

        @Override
        public String translate(String name) {
            String snake = super.translate(name);
            return snake == null ? null : snake.replaceAll("([a-z])(\\d)", "$1_$2");
        }
    }
}
