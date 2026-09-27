package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Maps one real row from each Open5e API endpoint (recorded in {@code import-fixtures/}) to columns. Every field must
 * land in a column and every value must be valid for its column's type, for all 33 endpoints, without network
 * access. The data itself isn't compared with the database, which changes whenever upstream content does.
 */
@SpringBootTest
class ImportMappingTest {

    @Autowired
    private DefaultContentImporter importer;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void everyEndpointMapsOntoItsTable() {
        List<String> tables = importer.tables();
        assertThat(tables).hasSize(33);

        Set<String> unknown = new TreeSet<>();
        List<String> invalid = new ArrayList<>();
        for (String table : tables) {
            Map<String, String> types = importer.columnTypes(table);
            Set<String> unknownFields = new TreeSet<>();
            Map<String, String> values = importer.toColumns(types, fixture(table), unknownFields);
            unknownFields.forEach(field -> unknown.add(table + "." + field));

            assertThat(values.get("key")).as(table + " key").isNotBlank();
            for (Map.Entry<String, String> column : values.entrySet()) {
                try {
                    jdbc.queryForObject("select cast(? as " + types.get(column.getKey()) + ") is not null",
                            Boolean.class, column.getValue());
                } catch (RuntimeException e) {
                    invalid.add(table + "." + column.getKey() + ": " + column.getValue());
                }
            }
        }

        assertThat(unknown).as("upstream fields with no column").isEmpty();
        assertThat(invalid).as("values not valid for their column's type").isEmpty();
    }

    @Test
    void fillsKeyColumnsFromObjectsAndKeys() {
        assertThat(columns("classes")).containsEntry("subclass_of_key", "a5e_marshal");
        assertThat(columns("items")).containsEntry("category_key", "weapon");
        assertThat(columns("species")).containsEntry("subspecies_of_key", "srd_halfling");
        assertThat(columns("abilities")).containsEntry("document_key", "core");        // document is a key here
        assertThat(columns("spells").get("document_key")).isNotBlank();                // and an object here
        assertThat(columns("spells").get("document")).startsWith("{");
    }

    @Test
    void mapsCamelCaseFieldsToSnakeCaseColumns() {
        assertThat(columns("rules")).containsEntry("initial_header_level", "2");
    }

    @Test
    void rewritesOpen5eLinksToThisApi() {
        assertThat(columns("itemsets").get("items"))
                .contains("\"url\":\"/api/items/srd_crystal\"")
                .doesNotContain("open5e.com");
    }

    @Test
    void neverSetsColumnsTheAppOwns() {
        for (String table : List.of("creatures", "documents")) {
            assertThat(columns(table)).doesNotContainKeys("derived_from", "owner_id");
        }
    }

    private Map<String, String> columns(String table) {
        return importer.toColumns(importer.columnTypes(table), fixture(table), new TreeSet<>());
    }

    private ObjectNode fixture(String table) {
        try {
            return (ObjectNode) jsonMapper.readTree(
                    new ClassPathResource("import-fixtures/" + table + ".json").getInputStream());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
