package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Saving what a source returns as a snapshot that changes only where the data did. */
class SnapshotWriterTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SnapshotWriter writer = new SnapshotWriter(mapper);

    /** A source with fixed rows; a row is {@code key:name}. */
    private DefaultContentSource source(Map<String, List<String>> tables) {
        return new DefaultContentSource() {
            @Override
            public List<ObjectNode> fetch(String endpoint) {
                List<ObjectNode> rows = new ArrayList<>();
                for (String row : tables.getOrDefault(endpoint, List.of())) {
                    String[] parts = row.split(":");
                    rows.add(mapper.createObjectNode().put("key", parts[0]).put("name", parts[1]));
                }
                return rows;
            }

            @Override
            public String describe() {
                return "test source";
            }
        };
    }

    private static Map<String, List<String>> tables(String table, String... rows) {
        Map<String, List<String>> map = new HashMap<>();
        map.put(table, List.of(rows));
        return map;
    }

    @Test
    void writesARowPerLineSortedByKey_andAManifest(@TempDir Path folder) throws IOException {
        List<SnapshotWriter.Change> changes = writer.write(source(tables("spells", "b:Beta", "a:Alpha")), List.of("spells"), folder, false, true);

        assertThat(Files.readAllLines(folder.resolve("spells.jsonl"))).containsExactly(
                "{\"key\":\"a\",\"name\":\"Alpha\"}", "{\"key\":\"b\",\"name\":\"Beta\"}");
        assertThat(changes).containsExactly(new SnapshotWriter.Change("spells", 2, 2, 0, 0));
        String manifest = Files.readString(folder.resolve("manifest.json"));
        assertThat(manifest).contains("\"sourceUrl\" : \"test source\"").contains("\"spells\" : 2").contains("fetchedAt");
    }

    @Test
    void aSecondRefreshThatFindsNothingNewChangesNothingAtAll(@TempDir Path folder) throws IOException {
        DefaultContentSource same = source(tables("spells", "a:Alpha", "b:Beta"));
        writer.write(same, List.of("spells"), folder, false, true);
        long spells = Files.getLastModifiedTime(folder.resolve("spells.jsonl")).toMillis();
        String manifest = Files.readString(folder.resolve("manifest.json"));

        List<SnapshotWriter.Change> changes = writer.write(same, List.of("spells"), folder, false, true);

        assertThat(changes.getFirst().any()).isFalse();
        assertThat(Files.getLastModifiedTime(folder.resolve("spells.jsonl")).toMillis()).isEqualTo(spells);
        assertThat(Files.readString(folder.resolve("manifest.json"))).isEqualTo(manifest);
    }

    @Test
    void reportsRowsAddedChangedAndRemoved(@TempDir Path folder) throws IOException {
        writer.write(source(tables("spells", "a:Alpha", "b:Beta", "c:Gamma")), List.of("spells"), folder, false, true);

        List<SnapshotWriter.Change> changes = writer.write(source(tables("spells", "a:Alpha", "b:Bravo", "d:Delta", "e:Epsilon")),
                List.of("spells"), folder, false, true);

        assertThat(changes).containsExactly(new SnapshotWriter.Change("spells", 4, 2, 1, 1)); // d and e; b; c
        assertThat(Files.readAllLines(folder.resolve("spells.jsonl"))).hasSize(4).contains("{\"key\":\"b\",\"name\":\"Bravo\"}");
    }

    @Test
    void refusesATableThatShrankToLessThanHalf_andWritesNothing(@TempDir Path folder) throws IOException {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            many.add("k" + String.format("%02d", i) + ":n");
        }
        writer.write(source(Map.of("spells", many, "rules", List.of("r:Rule"))), List.of("spells", "rules"), folder, false, true);
        String before = Files.readString(folder.resolve("spells.jsonl"));

        assertThatThrownBy(() -> writer.write(source(Map.of("spells", List.of("k00:n"), "rules", List.of("r:Changed"))),
                List.of("spells", "rules"), folder, false, true))
                .hasMessageContaining("only 1 spells")
                .hasMessageContaining("nothing was written");

        assertThat(Files.readString(folder.resolve("spells.jsonl"))).isEqualTo(before);
        assertThat(Files.readString(folder.resolve("rules.jsonl"))).contains("Rule"); // the other table wasn't touched either
    }

    @Test
    void acceptsThatShrinkWhenTold(@TempDir Path folder) throws IOException {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            many.add("k" + String.format("%02d", i) + ":n");
        }
        writer.write(source(Map.of("spells", many)), List.of("spells"), folder, false, true);

        writer.write(source(Map.of("spells", List.of("k00:n"))), List.of("spells"), folder, true, true);

        assertThat(Files.readAllLines(folder.resolve("spells.jsonl"))).hasSize(1);
    }

    @Test
    void refreshingSomeTablesLeavesTheOthersAndTheirCounts(@TempDir Path folder) throws IOException {
        writer.write(source(Map.of("spells", List.of("a:Alpha"), "rules", List.of("r:Rule"))), List.of("spells", "rules"), folder, false, true);

        writer.write(source(Map.of("spells", List.of("a:Alpha", "b:Beta"))), List.of("spells"), folder, false, false);

        assertThat(Files.readAllLines(folder.resolve("rules.jsonl"))).containsExactly("{\"key\":\"r\",\"name\":\"Rule\"}");
        String manifest = Files.readString(folder.resolve("manifest.json"));
        assertThat(manifest).contains("\"spells\" : 2").contains("\"rules\" : 1");
    }

    @Test
    void removesTheFileOfATableThatNoLongerExists_onlyWhenEveryTableWasRefreshed(@TempDir Path folder) throws IOException {
        writer.write(source(Map.of("spells", List.of("a:Alpha"), "old", List.of("o:Old"))), List.of("spells", "old"), folder, false, true);

        writer.write(source(Map.of("spells", List.of("a:Alpha"))), List.of("spells"), folder, false, false);
        assertThat(folder.resolve("old.jsonl")).exists();

        writer.write(source(Map.of("spells", List.of("a:Alpha"))), List.of("spells"), folder, false, true);
        assertThat(folder.resolve("old.jsonl")).doesNotExist();
    }

    @Test
    void whatItWritesCanBeReadBack(@TempDir Path folder) {
        writer.write(source(tables("spells", "b:Beta", "a:Alpha")), List.of("spells"), folder, false, true);

        SnapshotSource back = new SnapshotSource(mapper, "file:" + folder.toAbsolutePath().toString().replace('\\', '/'));

        assertThat(back.fetch("spells")).extracting(row -> row.get("key").asString()).containsExactly("a", "b");
    }

    @Test
    void refusesARowWithNoKey_orTwoWithTheSame(@TempDir Path folder) {
        DefaultContentSource keyless = new DefaultContentSource() {
            @Override
            public List<ObjectNode> fetch(String endpoint) {
                return List.of(mapper.createObjectNode().put("name", "x"));
            }

            @Override
            public String describe() {
                return "keyless";
            }
        };

        assertThatThrownBy(() -> writer.write(keyless, List.of("spells"), folder, false, true)).hasMessageContaining("has no key");
        assertThatThrownBy(() -> writer.write(source(tables("spells", "a:One", "a:Two")), List.of("spells"), folder, false, true))
                .hasMessageContaining("Two spells rows have the key 'a'");
    }
}
