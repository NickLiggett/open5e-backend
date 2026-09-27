package com.main.app.common.json;

import com.main.app.common.Entities;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For every entity, checks that each {@code jsonb} column survives Hibernate's mapping unchanged: the entity's value
 * is written back out as snake_case JSON and compared with the raw column. A dropped field, a lossy type or an
 * unrecognised enum value shows up as a mismatch. Explicit nulls and absent keys are treated as equal.
 * <p>
 * Covers every row of default content, and new entities automatically.
 */
@SpringBootTest
@Transactional(readOnly = true)
class JsonColumnsRoundTripTest {

    /** Embedded copies of other rows that the API deliberately returns as {key, name} references. */
    private static final Set<String> REDUCED_TO_REFERENCES = Set.of(
            "abilities.skills", "creaturesets.creatures", "itemsets.items", "rulesets.rules");

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyJsonColumnRoundTrips() {
        List<String> mismatches = new ArrayList<>();
        int checked = 0;
        for (Class<?> entity : Entities.all(em)) {
            Map<String, Field> columns = Entities.jsonColumns(entity);
            if (columns.isEmpty()) {
                continue;
            }
            String table = Entities.table(entity);
            Field keyField = Entities.field(entity, "key").orElseThrow();
            Map<Object, Object> byKey = new HashMap<>();
            for (Object row : em.createQuery("select e from " + entity.getSimpleName() + " e").getResultList()) {
                row = Hibernate.unproxy(row); // may already be a lazy proxy from an earlier entity's document
                byKey.put(Entities.get(keyField, row), row);
            }

            String select = columns.keySet().stream().map(c -> c + "::text")
                    .collect(Collectors.joining(", ", "select key, ", " from open5e." + table
                            + " where " + Entities.anonymouslyVisible(entity)));
            int[] rows = {0};
            jdbc.query(select, rs -> {
                rows[0]++;
                Object row = byKey.get(rs.getString("key"));
                if (row == null) {
                    mismatches.add(table + "." + rs.getString("key") + ": not loaded");
                    return;
                }
                int index = 2;
                for (Map.Entry<String, Field> column : columns.entrySet()) {
                    String raw = rs.getString(index++);
                    JsonNode expected = normalize(raw == null ? DatabaseJson.MAPPER.nullNode() : DatabaseJson.MAPPER.readTree(raw));
                    if (REDUCED_TO_REFERENCES.contains(table + "." + column.getKey())) {
                        expected = toReferences(expected);
                    }
                    JsonNode actual = normalize(DatabaseJson.MAPPER.valueToTree(Entities.get(column.getValue(), row)));
                    if (!expected.equals(actual)) {
                        mismatches.add(table + "." + rs.getString("key") + "." + column.getKey()
                                + "\n  expected: " + expected + "\n  actual:   " + actual);
                    }
                }
            });
            assertEquals(rows[0], byKey.size(), table + ": loaded entity count doesn't match the visible row count");
            checked += rows[0];
        }

        assertTrue(checked > 0, "no rows checked");
        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches, first ones:\n"
                + String.join("\n", mismatches.subList(0, Math.min(10, mismatches.size()))));
    }

    /** Keeps only key and name of each element. */
    private static JsonNode toReferences(JsonNode array) {
        ArrayNode references = DatabaseJson.MAPPER.createArrayNode();
        array.forEach(element -> {
            ObjectNode reference = references.addObject();
            reference.set("key", element.get("key"));
            reference.set("name", element.get("name"));
        });
        return normalize(references);
    }

    /** Drops null-valued object fields. */
    private static JsonNode normalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode copy = DatabaseJson.MAPPER.createObjectNode();
            node.properties().forEach(e -> {
                if (e.getValue() != null && !e.getValue().isNull()) {
                    copy.set(e.getKey(), normalize(e.getValue()));
                }
            });
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = DatabaseJson.MAPPER.createArrayNode();
            node.forEach(child -> copy.add(normalize(child)));
            return copy;
        }
        return node;
    }
}
