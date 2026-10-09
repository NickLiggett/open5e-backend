package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading a snapshot of the Open5e API from a folder. */
class SnapshotSourceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private SnapshotSource at(Path folder) {
        return new SnapshotSource(mapper, "file:" + folder.toAbsolutePath().toString().replace('\\', '/'));
    }

    @Test
    void readsARowPerLine_skippingBlankLines(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("spells.jsonl"), "{\"key\":\"a\",\"name\":\"Alpha\"}\n\n{\"key\":\"b\",\"name\":\"Beta\"}\n");

        List<ObjectNode> rows = at(folder).fetch("spells");

        assertThat(rows).extracting(row -> row.get("key").asString()).containsExactly("a", "b");
        assertThat(rows.getFirst().get("name").asString()).isEqualTo("Alpha");
    }

    @Test
    void anEmptyFileIsATableWithNoRows(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("services.jsonl"), "");

        assertThat(at(folder).fetch("services")).isEmpty();
    }

    @Test
    void aMissingFileSaysWhichAndWhatToDo(@TempDir Path folder) {
        assertThatThrownBy(() -> at(folder).fetch("spells"))
                .hasMessageContaining("no spells.jsonl")
                .hasMessageContaining("refresh the snapshot");
    }

    @Test
    void aFileThatDoesntHaveTheRowsTheManifestSaysIsRefused(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("spells.jsonl"), "{\"key\":\"a\"}\n");
        Files.writeString(folder.resolve("manifest.json"), "{\"fetchedAt\":\"2026-10-09T01:16:08Z\",\"tables\":{\"spells\":2}}");

        assertThatThrownBy(() -> at(folder).fetch("spells")).hasMessageContaining("has 1 rows, but its manifest says 2");
    }

    @Test
    void sayswhenAndWhereItWasTaken(@TempDir Path folder) throws IOException {
        Files.writeString(folder.resolve("manifest.json"),
                "{\"fetchedAt\":\"2026-10-09T01:16:08Z\",\"sourceUrl\":\"https://api.open5e.com/v2\",\"tables\":{}}");

        assertThat(at(folder).describe()).isEqualTo("the Open5e snapshot of 2026-10-09T01:16:08Z (from https://api.open5e.com/v2)");
    }

    @Test
    void withoutAManifestItSaysWhereItIs(@TempDir Path folder) {
        assertThat(at(folder).describe()).startsWith("the Open5e snapshot at file:");
    }
}
