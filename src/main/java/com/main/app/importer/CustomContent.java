package com.main.app.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Content that Open5e doesn't offer, kept as JSON files so it can be reviewed and changed like code, rather than in a
 * database dump. There are two kinds, which differ in who can see them:
 * <ul>
 *     <li><b>Default content</b> (<code>custom-content/*.json</code> in this project): visible to everyone, like Open5e's
 *     own, and in this (public) repository. Only for content that can be published: your own, or openly licensed.</li>
 *     <li><b>Private content</b> (the folder named by {@code custom-content.private-dir}, never in git): each file has an
 *     {@code "owner"}, a username, and its documents belong to that user. Only they can see them, until they share them
 *     like any of their documents (the Sharing page). For anything that isn't ours to publish, such as a book's rules.</li>
 * </ul>
 * A file is an object with one array per Open5e endpoint, each row in the API's own shape, the same as what the importer
 * reads from Open5e:
 * <pre>
 * { "owner": "nick",   (private content only)
 *   "documents": [ { "key": "my-source", "name": "...", "type": "SOURCE", ... } ],
 *   "species":   [ { "key": "my-source_thing", "document": "my-source", "name": "...", "traits": [...] } ] }
 * </pre>
 * Rules that keep content apart from Open5e's, from users' own and from each other, so none can overwrite another:
 * <ul>
 *     <li>every row (outside {@code documents}) belongs to a document defined in its own file (default content: in the
 *     default files); custom content never adds to Open5e's documents or to a user's</li>
 *     <li>{@code "document"} can be just that document's key; it is expanded to the full object Open5e rows have</li>
 *     <li>a key can't be used twice, within a table, anywhere in custom content</li>
 * </ul>
 * The importer merges these rows as if they were upstream: added, updated when the file changes, and deleted when they
 * are removed from the file. See {@link LayeredContentSource}, {@link PrivateContentImporter} and
 * {@code open5e.import.content}.
 */
@Component
public class CustomContent {

    static final String PATTERN = "classpath*:custom-content/*.json";
    /** Document keys go into SQL text (for the custom-only import), so they are kept to these characters. */
    static final Pattern DOCUMENT_KEY = Pattern.compile("[a-z0-9][a-z0-9._-]*");
    /** The usernames the backend makes from a sign-in (see UserService). */
    static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    private static final List<String> DOCUMENT_SUMMARY_FIELDS =
            List.of("key", "name", "type", "permalink", "publisher", "gamesystem", "display_name");

    /** One private file: whose it is, and what is in it. */
    public record Private(String owner, String file, ContentSet content) {
    }

    private record Parsed(String name, ObjectNode root) {
    }

    private final ContentSet shared;
    private final List<Private> privateContent;

    @Autowired
    public CustomContent(JsonMapper mapper, @Value("${custom-content.private-dir:}") String privateDir) {
        this(mapper, find(), findPrivate(privateDir));
    }

    /** From these files, in this order. For tests. */
    CustomContent(JsonMapper mapper, List<Resource> files) {
        this(mapper, files, List.of());
    }

    CustomContent(JsonMapper mapper, List<Resource> files, List<Resource> privateFiles) {
        List<Parsed> sharedFiles = new ArrayList<>();
        for (Resource file : files) {
            Parsed parsed = read(mapper, file);
            if (parsed.root().has("owner")) {
                throw new IllegalStateException("Custom content " + parsed.name() + " has an owner, which makes it private: "
                        + "it belongs in the private content folder, not in the repository");
            }
            sharedFiles.add(parsed);
        }
        this.shared = build(mapper, sharedFiles);

        List<Private> found = new ArrayList<>();
        for (Resource file : privateFiles) {
            Parsed parsed = read(mapper, file);
            String owner = parsed.root().path("owner").isString() ? parsed.root().get("owner").asString() : "";
            if (!USERNAME.matcher(owner).matches()) {
                throw new IllegalStateException("Private content " + parsed.name() + " needs an \"owner\": the username of "
                        + "the user it belongs to, in lower case, e.g. \"nick\"");
            }
            parsed.root().remove("owner");
            found.add(new Private(owner, parsed.name(), build(mapper, List.of(parsed))));
        }
        this.privateContent = List.copyOf(found);
        checkForClashes();
    }

    private static List<Resource> find() {
        try {
            Resource[] found = new PathMatchingResourcePatternResolver().getResources(PATTERN);
            return java.util.Arrays.stream(found).sorted(Comparator.comparing(r -> String.valueOf(r.getFilename()))).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Resource> findPrivate(String directory) {
        if (directory == null || directory.isBlank()) {
            return List.of();
        }
        Path path = Path.of(directory.trim());
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("custom-content.private-dir is '" + directory + "', which isn't a folder");
        }
        try (Stream<Path> files = Files.list(path)) {
            return files.filter(file -> file.getFileName().toString().endsWith(".json")).sorted()
                    .<Resource>map(FileSystemResource::new).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Parsed read(JsonMapper mapper, Resource file) {
        String name = String.valueOf(file.getFilename());
        JsonNode root;
        try (InputStream in = file.getInputStream()) {
            root = mapper.readTree(in);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Custom content " + name + " can't be read: " + e.getMessage(), e);
        }
        if (!(root instanceof ObjectNode object)) {
            throw new IllegalStateException("Custom content " + name + " must be an object with an array per endpoint");
        }
        return new Parsed(name, object);
    }

    /** The rows of these files, checked, with each row's document made the full object Open5e rows have. */
    private static ContentSet build(JsonMapper mapper, List<Parsed> files) {
        Map<String, List<Map.Entry<String, ObjectNode>>> pending = new LinkedHashMap<>();
        for (Parsed file : files) {
            for (Map.Entry<String, JsonNode> endpoint : file.root().properties()) {
                if (!endpoint.getValue().isArray()) {
                    throw new IllegalStateException("Custom content " + file.name() + ": '" + endpoint.getKey() + "' must be an array of rows");
                }
                for (JsonNode row : endpoint.getValue()) {
                    if (!(row instanceof ObjectNode object) || !object.path("key").isString() || object.get("key").asString().isBlank()) {
                        throw new IllegalStateException("Custom content " + file.name() + ": a row in '" + endpoint.getKey() + "' has no key");
                    }
                    pending.computeIfAbsent(endpoint.getKey(), k -> new ArrayList<>()).add(Map.entry(file.name(), object));
                }
            }
        }

        Map<String, ObjectNode> documents = new LinkedHashMap<>();
        for (Map.Entry<String, ObjectNode> entry : pending.getOrDefault("documents", List.of())) {
            String key = entry.getValue().get("key").asString();
            if (!DOCUMENT_KEY.matcher(key).matches()) {
                throw new IllegalStateException("Custom content " + entry.getKey() + ": document key '" + key
                        + "' can only have lowercase letters, digits, '.', '_' and '-'");
            }
            if (documents.put(key, entry.getValue()) != null) {
                throw new IllegalStateException("Custom content has the document '" + key + "' twice");
            }
        }

        Map<String, List<ObjectNode>> rows = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map.Entry<String, ObjectNode>>> table : pending.entrySet()) {
            Set<String> keys = new LinkedHashSet<>();
            List<ObjectNode> tableRows = new ArrayList<>();
            for (Map.Entry<String, ObjectNode> entry : table.getValue()) {
                ObjectNode row = entry.getValue().deepCopy();
                String key = row.get("key").asString();
                if (!keys.add(key)) {
                    throw new IllegalStateException("Custom content has the key '" + key + "' twice in '" + table.getKey() + "'");
                }
                if (!table.getKey().equals("documents")) {
                    attachDocument(entry.getKey(), table.getKey(), row, documents, mapper);
                }
                tableRows.add(row);
            }
            rows.put(table.getKey(), List.copyOf(tableRows));
        }
        return new ContentSet(rows, Set.copyOf(documents.keySet()));
    }

    /** No document and no row key in two places: the default files and each private file are checked against each other. */
    private void checkForClashes() {
        Map<String, String> documentIn = new HashMap<>();
        Map<String, String> rowIn = new HashMap<>();
        List<Map.Entry<String, ContentSet>> sets = new ArrayList<>();
        sets.add(Map.entry("the default custom content", shared));
        privateContent.forEach(one -> sets.add(Map.entry("private content " + one.file(), one.content())));
        for (Map.Entry<String, ContentSet> set : sets) {
            for (String document : set.getValue().documentKeys()) {
                String before = documentIn.put(document, set.getKey());
                if (before != null) {
                    throw new IllegalStateException("The document '" + document + "' is in " + before + " and in " + set.getKey());
                }
            }
            for (String table : set.getValue().tables()) {
                for (ObjectNode row : set.getValue().rows(table)) {
                    String id = table + ":" + row.get("key").asString();
                    String before = rowIn.put(id, set.getKey());
                    if (before != null) {
                        throw new IllegalStateException("The " + table + " key '" + row.get("key").asString() + "' is in "
                                + before + " and in " + set.getKey());
                    }
                }
            }
        }
    }

    /** Checks the row's document is one of its own and, when given as a key, expands it to Open5e's embedded object. */
    private static void attachDocument(String file, String table, ObjectNode row, Map<String, ObjectNode> documents,
                                       JsonMapper mapper) {
        JsonNode document = row.get("document");
        String key = document == null ? null
                : document.isString() ? document.asString()
                : document.isObject() && document.path("key").isString() ? document.get("key").asString()
                : null;
        if (key == null) {
            throw new IllegalStateException("Custom content " + file + ": '" + row.get("key").asString() + "' in '" + table
                    + "' has no document");
        }
        ObjectNode definition = documents.get(key);
        if (definition == null) {
            throw new IllegalStateException("Custom content " + file + ": '" + row.get("key").asString() + "' in '" + table
                    + "' is in the document '" + key + "', which these files don't define. Custom content can't add to "
                    + "Open5e's documents or anyone's own");
        }
        if (document.isString()) {
            ObjectNode summary = mapper.createObjectNode();
            for (String field : DOCUMENT_SUMMARY_FIELDS) {
                if (definition.has(field)) {
                    summary.set(field, definition.get(field).deepCopy());
                }
            }
            row.set("document", summary);
        }
    }

    /** The default content as a set. */
    ContentSet shared() {
        return shared;
    }

    // The default content, which is what the rest of the importer has always asked for.

    /** The default rows for an Open5e endpoint, none if there are none. Each call gives copies. */
    public List<ObjectNode> rows(String endpoint) {
        return shared.rows(endpoint);
    }

    /** The endpoints (tables) the default files have rows for. */
    public List<String> tables() {
        return shared.tables();
    }

    /** The keys of the documents the default files define. */
    public Set<String> documentKeys() {
        return shared.documentKeys();
    }

    public int size() {
        return shared.size();
    }

    /** Just the default content, as an import source (for the custom-only import). */
    public DefaultContentSource asSource() {
        return shared.asSource("custom content");
    }

    /** The private files, each with its owner. Empty unless {@code custom-content.private-dir} is set. */
    public List<Private> privateContent() {
        return privateContent;
    }
}
