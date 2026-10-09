package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The snapshot of the Open5e API that is committed ({@code src/main/resources/open5e-snapshot}): it has every table the
 * schema has, each file is whole, and the importer takes it. A new table in the schema, or a damaged file, fails here
 * rather than in the middle of someone's import.
 */
@SpringBootTest
class OpenSnapshotTest {

    @Autowired
    private DefaultContentImporter importer;

    @Autowired
    private JsonMapper mapper;

    private SnapshotSource snapshot() {
        return new SnapshotSource(mapper, SnapshotSource.DEFAULT_LOCATION);
    }

    @Test
    void hasEveryTableOfTheSchema_eachWithRowsThatHaveKeys() {
        for (String table : importer.tables()) {
            List<ObjectNode> rows = snapshot().fetch(table); // also checks the file against the manifest's count
            assertThat(rows).as(table).isNotEmpty();
            Set<String> keys = new HashSet<>();
            for (ObjectNode row : rows) {
                assertThat(row.path("key").isString()).as(table + " row has a key").isTrue();
                assertThat(keys.add(row.get("key").asString())).as(table + " key " + row.get("key").asString() + " is unique").isTrue();
            }
        }
    }

    @Test
    void everyRowsDocumentIsInTheSnapshot() {
        Set<String> documents = new HashSet<>();
        snapshot().fetch("documents").forEach(row -> documents.add(row.get("key").asString()));

        for (String table : importer.tables()) {
            for (ObjectNode row : snapshot().fetch(table)) {
                JsonNode document = row.path("document");
                if (document.isObject()) {
                    assertThat(documents).as(table + " " + row.get("key").asString() + "'s document").contains(document.path("key").asString());
                }
            }
        }
    }

    @Test
    void theImporterTakesItAsItIs_withNoFieldLeftWithoutAColumn() {
        ImportReport report = importer.run(snapshot(), false, false);

        assertThat(report.tables()).hasSize(importer.tables().size());
        assertThat(report.tables()).allSatisfy(table -> assertThat(table.unknownFields()).as(table.name()).isEmpty());
    }
}
