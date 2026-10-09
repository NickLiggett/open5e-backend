package com.main.app.importer;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Saves what a source returns as a snapshot a {@link SnapshotSource} can read: for each table a file of one row per line,
 * sorted by key and written the same way every time, so that refreshing a snapshot changes only the lines whose rows
 * changed, and a refresh that finds nothing new changes nothing at all.
 * <p>
 * Nothing is written unless every table was fetched, so a failure part-way leaves the old snapshot as it was. A table
 * that comes back with less than half its previous rows is refused, as the importer does, unless told otherwise.
 */
@Lazy
@Component
public class SnapshotWriter {

    /** What a refresh did to one table's file. */
    public record Change(String table, int rows, int added, int changed, int removed) {
        public boolean any() {
            return added + changed + removed > 0;
        }
    }

    private final JsonMapper mapper;

    public SnapshotWriter(JsonMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param source   where the rows come from
     * @param tables   the endpoints to save
     * @param folder   the snapshot's folder; made if it isn't there
     * @param allowLargeShrink save a table even when it has less than half its previous rows
     * @param everyTable whether {@code tables} are all of them: if so, files of tables that no longer exist are removed; if
     *                   not (a refresh of a few tables, to ask Open5e for a little at a time), the other tables are left
     *                   as they were
     * @return what changed in each table, in the order of {@code tables}
     */
    public List<Change> write(DefaultContentSource source, List<String> tables, Path folder, boolean allowLargeShrink, boolean everyTable) {
        Map<String, List<ObjectNode>> fetched = new TreeMap<>();
        List<Change> changes = new ArrayList<>();
        for (String table : tables) {
            List<ObjectNode> rows = sorted(table, source.fetch(table));
            Map<String, ObjectNode> before = read(folder.resolve(table + ".jsonl"));
            Change change = compare(table, rows, before);
            if (!allowLargeShrink && before.size() >= 10 && rows.size() * 2 < before.size()) {
                throw new IllegalStateException("Open5e returned only " + rows.size() + " " + table + ", where the snapshot has "
                        + before.size() + ". That looks like a failure, not a change; nothing was written. Add "
                        + "--open5e.import.allow-large-deletions=true if it is real");
            }
            fetched.put(table, rows);
            changes.add(change);
        }

        try {
            Files.createDirectories(folder);
            for (Map.Entry<String, List<ObjectNode>> table : fetched.entrySet()) {
                writeIfChanged(folder.resolve(table.getKey() + ".jsonl"), lines(table.getValue()));
            }
            if (everyTable) {
                removeFilesOfTablesThatAreGone(folder, Set.copyOf(tables));
            }
            writeManifest(folder, source, fetched, changes, everyTable);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return changes;
    }

    private List<ObjectNode> sorted(String table, List<ObjectNode> rows) {
        Map<String, ObjectNode> byKey = new TreeMap<>();
        for (ObjectNode row : rows) {
            JsonNode key = row.get("key");
            if (key == null || !key.isString()) {
                throw new IllegalStateException("A " + table + " row has no key, so it can't be kept in order: " + row.toString().substring(0, Math.min(120, row.toString().length())));
            }
            if (byKey.put(key.asString(), row) != null) {
                throw new IllegalStateException("Two " + table + " rows have the key '" + key.asString() + "'");
            }
        }
        return List.copyOf(byKey.values());
    }

    private Change compare(String table, List<ObjectNode> rows, Map<String, ObjectNode> before) {
        int added = 0;
        int changed = 0;
        for (ObjectNode row : rows) {
            ObjectNode old = before.get(row.get("key").asString());
            if (old == null) {
                added++;
            } else if (!old.equals(row)) {
                changed++;
            }
        }
        Set<String> now = new java.util.HashSet<>();
        rows.forEach(row -> now.add(row.get("key").asString()));
        int removed = (int) before.keySet().stream().filter(key -> !now.contains(key)).count();
        return new Change(table, rows.size(), added, changed, removed);
    }

    private Map<String, ObjectNode> read(Path file) {
        Map<String, ObjectNode> rows = new HashMap<>();
        if (!Files.exists(file)) {
            return rows;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    ObjectNode row = (ObjectNode) mapper.readTree(line);
                    rows.put(row.path("key").asString(), row);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
        return rows;
    }

    private String lines(List<ObjectNode> rows) {
        StringBuilder text = new StringBuilder();
        for (ObjectNode row : rows) {
            text.append(mapper.writeValueAsString(row)).append('\n');
        }
        return text.toString();
    }

    /** Leaves a file that already says this alone, so that its time isn't changed for nothing. */
    private void writeIfChanged(Path file, String content) throws IOException {
        if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content)) {
            return;
        }
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8);
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
    }

    private void removeFilesOfTablesThatAreGone(Path folder, Set<String> tables) throws IOException {
        try (Stream<Path> files = Files.list(folder)) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".jsonl")).toList()) {
                String name = file.getFileName().toString();
                if (!tables.contains(name.substring(0, name.length() - ".jsonl".length()))) {
                    Files.delete(file);
                }
            }
        }
    }

    /** The manifest says when and from where; a refresh that changed nothing leaves it as it was, so git sees no change. */
    private void writeManifest(Path folder, DefaultContentSource source, Map<String, List<ObjectNode>> fetched, List<Change> changes,
                               boolean everyTable) throws IOException {
        Path file = folder.resolve(SnapshotSource.MANIFEST);
        boolean unchanged = changes.stream().noneMatch(Change::any);
        if (Files.exists(file) && unchanged) {
            return;
        }
        // When only some tables were fetched, the others keep the counts they had.
        Map<String, Integer> kept = new TreeMap<>();
        if (!everyTable && Files.exists(file)) {
            mapper.readTree(Files.readString(file, StandardCharsets.UTF_8)).path("tables").properties()
                    .forEach(entry -> kept.put(entry.getKey(), entry.getValue().asInt()));
        }
        ObjectNode manifest = mapper.createObjectNode();
        manifest.put("fetchedAt", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
        manifest.put("sourceUrl", source.describe());
        ObjectNode counts = manifest.putObject("tables");
        fetched.forEach((table, rows) -> kept.put(table, rows.size()));
        kept.forEach(counts::put);
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(manifest) + "\n", StandardCharsets.UTF_8);
    }
}
