package com.main.app.common.json;

import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the JSON columns of a table survive the app unchanged: for every row, each mapped value is written back
 * out as snake_case JSON and compared with the raw column in the database. A dropped field, a lossy type or an
 * unrecognised enum value shows up as a mismatch. Explicit nulls and absent keys are treated as equal.
 */
public final class JsonColumnRoundTrip {

    private JsonColumnRoundTrip() {
    }

    /**
     * @param mapped  the app's objects for every row of the table, by key
     * @param columns JSON column name → the value the app produced for it
     */
    public static <T> void assertRoundTrips(JdbcTemplate jdbc, String table, Map<String, T> mapped,
                                            Map<String, Function<T, Object>> columns) {
        String select = columns.keySet().stream()
                .map(column -> column + "::text")
                .collect(Collectors.joining(", ", "select key, ", " from open5e." + table));

        List<String> mismatches = new ArrayList<>();
        int[] rows = {0};
        jdbc.query(select, rs -> {
            rows[0]++;
            String key = rs.getString("key");
            T value = mapped.get(key);
            if (value == null) {
                mismatches.add(key + ": row was not mapped");
                return;
            }
            int index = 2;
            for (Map.Entry<String, Function<T, Object>> column : columns.entrySet()) {
                String raw = rs.getString(index++);
                JsonNode expected = withoutNulls(raw == null ? DatabaseJson.MAPPER.nullNode() : DatabaseJson.MAPPER.readTree(raw));
                JsonNode actual = withoutNulls(DatabaseJson.MAPPER.valueToTree(column.getValue().apply(value)));
                if (!expected.equals(actual)) {
                    mismatches.add(key + "." + column.getKey() + "\n  expected: " + expected + "\n  actual:   " + actual);
                }
            }
        });

        assertEquals(rows[0], mapped.size(), "mapped object count doesn't match the " + table + " row count");
        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches across " + rows[0] + " " + table
                + " rows, first ones:\n" + String.join("\n", mismatches.subList(0, Math.min(10, mismatches.size()))));
    }

    private static JsonNode withoutNulls(JsonNode node) {
        if (node.isObject()) {
            ObjectNode copy = DatabaseJson.MAPPER.createObjectNode();
            node.properties().forEach(e -> {
                if (!e.getValue().isNull()) {
                    copy.set(e.getKey(), withoutNulls(e.getValue()));
                }
            });
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = DatabaseJson.MAPPER.createArrayNode();
            node.forEach(child -> copy.add(withoutNulls(child)));
            return copy;
        }
        return node;
    }
}
