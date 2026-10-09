package com.main.app.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads default content from a snapshot of the Open5e API: one file of JSON per endpoint ({@code spells.jsonl}, a row per
 * line, in the API's own shape) and a {@code manifest.json} that says when it was taken, from where, and how many rows
 * each endpoint had. The snapshot in this repository ({@code src/main/resources/open5e-snapshot}) is on the classpath, so
 * an import needs neither the network nor Open5e being up. {@link SnapshotWriter} makes one.
 * <p>
 * {@code open5e.snapshot.location} says where to read it from instead: any Spring resource location, such as
 * {@code file:/some/folder}.
 */
@Lazy
@Component
public class SnapshotSource implements DefaultContentSource {

    public static final String MANIFEST = "manifest.json";
    public static final String DEFAULT_LOCATION = "classpath:open5e-snapshot";

    private final JsonMapper mapper;
    private final String location;
    private final DefaultResourceLoader loader = new DefaultResourceLoader();

    @Autowired
    public SnapshotSource(JsonMapper mapper, @Value("${open5e.snapshot.location:" + DEFAULT_LOCATION + "}") String location) {
        this.mapper = mapper;
        this.location = location.replaceAll("/+$", "");
    }

    @Override
    public List<ObjectNode> fetch(String endpoint) {
        Resource file = loader.getResource(location + "/" + endpoint + ".jsonl");
        if (!file.exists()) {
            throw new IllegalStateException("The Open5e snapshot at " + location + " has no " + endpoint + ".jsonl. If the schema "
                    + "has a new table, refresh the snapshot (see \"Refreshing default content\" in the README)");
        }
        List<ObjectNode> rows = new ArrayList<>();
        try (BufferedReader lines = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = lines.readLine()) != null) {
                if (!line.isBlank()) {
                    rows.add((ObjectNode) mapper.readTree(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
        JsonNode expected = manifest().path("tables").path(endpoint);
        if (expected.isNumber() && expected.asInt() != rows.size()) {
            throw new IllegalStateException("The Open5e snapshot's " + endpoint + ".jsonl has " + rows.size() + " rows, but its manifest says "
                    + expected.asInt() + ": the file is damaged or half-updated. Restore it, or refresh the snapshot");
        }
        return rows;
    }

    @Override
    public String describe() {
        JsonNode manifest = manifest();
        return manifest.path("fetchedAt").isString()
                ? "the Open5e snapshot of " + manifest.get("fetchedAt").asString() + " (from " + manifest.path("sourceUrl").asString("Open5e") + ")"
                : "the Open5e snapshot at " + location;
    }

    private JsonNode manifest() {
        Resource file = loader.getResource(location + "/" + MANIFEST);
        if (!file.exists()) {
            return mapper.createObjectNode();
        }
        try (var in = file.getInputStream()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Can't read " + file, e);
        }
    }
}
