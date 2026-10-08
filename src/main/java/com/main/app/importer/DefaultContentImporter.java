package com.main.app.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Refreshes default content from the Open5e API without touching users' content.
 * <p>
 * The tables mirror the API: each endpoint is a table, and each field a column of the same name. Differences are
 * resolved generically: a field {@code x} also fills an {@code x_key} column with its key (e.g. {@code document}
 * → {@code document_key}), and camelCase fields fill snake_case columns ({@code initialHeaderLevel}). Links to other
 * Open5e resources become paths on this API ({@link ApiUrls}).
 * <p>
 * For each table, upstream rows replace the default rows with the same key (unchanged rows aren't written), new
 * ones are added, and default rows upstream no longer has are deleted. Rows in user documents are never written,
 * even if an upstream key collides with one; user copies of deleted rows keep their data. The whole import is one
 * transaction, which is the only place the database's protection of default content (V2) is lifted.
 * <p>
 * This is the one piece of application code that writes resource tables with plain SQL (bypassing the visibility
 * filter), which is safe because it only ever touches default content.
 */
@Service
public class DefaultContentImporter {

    private static final Logger log = LoggerFactory.getLogger(DefaultContentImporter.class);

    /** Tables that aren't Open5e content. Any table that is not an Open5e endpoint must be listed here. */
    private static final Set<String> NOT_CONTENT = Set.of("flyway_schema_history", "users", "document_members",
            "user_settings", "user_avatars", "user_tracker_states", "document_invitations", "player_characters", "party_members");
    /** Columns the app owns; upstream never sets them. */
    private static final Set<String> APP_COLUMNS = Set.of("derived_from", "owner_id");
    private static final Pattern CAMEL = Pattern.compile("([a-z0-9])([A-Z])");
    /** Refuse to delete more than this share of a table's default rows unless told to. */
    private static final double MAX_DELETED_SHARE = 0.5;
    private static final int SMALL_TABLE = 10;

    /**
     * Which content an import may add to, change and delete: the rows of these documents, and nothing else.
     *
     * @param documents the keys of the documents it covers
     * @param ownerId   the user who owns them, or null for default content (documents with no owner)
     */
    public record Scope(Set<String> documents, Long ownerId) {

        public static Scope defaultContent(Set<String> documents) {
            return new Scope(documents, null);
        }

        public static Scope ownedBy(long ownerId, Set<String> documents) {
            return new Scope(documents, ownerId);
        }
    }

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public DefaultContentImporter(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    /** The content tables, which are also the Open5e endpoints: documents first, then alphabetical. */
    public List<String> tables() {
        List<String> tables = new ArrayList<>(jdbc.queryForList("""
                select table_name from information_schema.tables
                where table_schema = 'open5e' and table_type = 'BASE TABLE' order by table_name""", String.class));
        tables.removeAll(NOT_CONTENT);
        tables.remove("documents");
        tables.addFirst("documents");
        return tables;
    }

    /**
     * Fetches everything from the source, then merges it in one transaction.
     *
     * @param apply               false for a dry run: report what would change, then roll back
     * @param allowLargeDeletions allow deleting more than half of a table's default rows (normally a sign that the
     *                            source returned too little)
     */
    public ImportReport run(DefaultContentSource source, boolean apply, boolean allowLargeDeletions) {
        return run(source, tables(), apply, allowLargeDeletions);
    }

    /** Like {@link #run(DefaultContentSource, boolean, boolean)}, for some tables only. */
    public ImportReport run(DefaultContentSource source, List<String> tables, boolean apply, boolean allowLargeDeletions) {
        return run(source, tables, apply, allowLargeDeletions, (Scope) null);
    }

    /**
     * Like {@link #run(DefaultContentSource, List, boolean, boolean)}, but only the default content in these documents
     * is ever added to, changed or deleted; Open5e's other default content is left exactly as it is. This is how a
     * source with just some documents' rows (custom content) is applied without being taken for "all of upstream".
     *
     * @param onlyDocuments keys of the documents to apply to, or null for all default content
     */
    public ImportReport run(DefaultContentSource source, List<String> tables, boolean apply, boolean allowLargeDeletions,
                            Set<String> onlyDocuments) {
        return run(source, tables, apply, allowLargeDeletions, onlyDocuments == null ? null : Scope.defaultContent(onlyDocuments));
    }

    /**
     * As {@link #run(DefaultContentSource, List, boolean, boolean, Set)}, for the content of {@code scope}: some
     * default documents, or some documents owned by a user. Nothing outside it is touched, and a row that would replace
     * one outside it is refused.
     */
    public ImportReport run(DefaultContentSource source, List<String> tables, boolean apply, boolean allowLargeDeletions,
                            Scope scope) {
        Map<String, List<ObjectNode>> content = new LinkedHashMap<>();
        for (String table : tables) {
            List<ObjectNode> rows = source.fetch(table);
            log.info("Fetched {} {} from {}", rows.size(), table, source.describe());
            content.put(table, rows);
        }
        List<ImportReport.Table> reports = transactions.execute(status -> {
            if (!apply) {
                status.setRollbackOnly();
            }
            return merge(content, allowLargeDeletions, scope);
        });
        return new ImportReport(source.describe(), apply, reports);
    }

    /**
     * Merges the given tables' upstream rows into the database. Tables not in {@code content} are left alone. Must run
     * inside a transaction, which decides whether the changes are kept.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ImportReport.Table> merge(Map<String, List<ObjectNode>> content, boolean allowLargeDeletions) {
        return merge(content, allowLargeDeletions, (Scope) null);
    }

    /** As {@link #merge(Map, boolean)}, touching only the default content of {@code onlyDocuments} (null: all of it). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ImportReport.Table> merge(Map<String, List<ObjectNode>> content, boolean allowLargeDeletions,
                                          Set<String> onlyDocuments) {
        return merge(content, allowLargeDeletions, onlyDocuments == null ? null : Scope.defaultContent(onlyDocuments));
    }

    /** As {@link #merge(Map, boolean)}, touching only the content of {@code scope} (null: all default content). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<ImportReport.Table> merge(Map<String, List<ObjectNode>> content, boolean allowLargeDeletions, Scope scope) {
        if (scope != null) {
            for (String key : scope.documents()) {
                if (!CustomContent.DOCUMENT_KEY.matcher(key).matches()) {
                    throw new IllegalArgumentException("Not a usable document key: " + key);
                }
            }
        }
        jdbc.execute("set local open5e.allow_default_content_changes = 'on'");
        // The API's timestamps have no time zone (e.g. 2024-01-01T00:00:00); read them as UTC wherever this runs.
        jdbc.execute("set local time zone 'UTC'");
        List<ImportReport.Table> reports = new ArrayList<>();
        for (Map.Entry<String, List<ObjectNode>> entry : content.entrySet()) {
            reports.add(mergeTable(entry.getKey(), entry.getValue(), allowLargeDeletions, scope));
        }
        return reports;
    }

    /**
     * An upstream row as column values: text for {@code cast(? as <type>)}, or null. Also reports fields with no
     * column.
     */
    Map<String, String> toColumns(Map<String, String> columnTypes, ObjectNode row, Set<String> unknownFields) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String column : columnTypes.keySet()) {
            if (!APP_COLUMNS.contains(column)) {
                values.put(column, null);
            }
        }
        for (Map.Entry<String, JsonNode> field : row.properties()) {
            String name = field.getKey();
            JsonNode value = field.getValue();
            boolean mapped = false;
            String column = columnTypes.containsKey(name) ? name : snakeCase(name);
            if (columnTypes.containsKey(column) && !APP_COLUMNS.contains(column)) {
                values.put(column, text(value, columnTypes.get(column), name));
                mapped = true;
            }
            String keyColumn = column + "_key";
            if (columnTypes.containsKey(keyColumn)) {
                JsonNode key = value.isObject() ? value.get("key") : value;
                values.put(keyColumn, key == null || key.isNull() ? null : key.asString());
                mapped = true;
            }
            if (!mapped) {
                unknownFields.add(name);
            }
        }
        return values;
    }

    private ImportReport.Table mergeTable(String table, List<ObjectNode> rows, boolean allowLargeDeletions, Scope scope) {
        Map<String, String> columnTypes = columnTypes(table);
        if (columnTypes.isEmpty()) {
            throw new IllegalArgumentException("No table open5e." + table);
        }
        boolean documents = table.equals("documents");
        boolean owned = columnTypes.containsKey("document_key");
        if (scope != null && !documents && !owned) {
            throw new IllegalStateException(table + " has no documents, so its content can't be limited to some documents");
        }
        // The keys were checked to be plain characters, and the owner is a number, so they can go into the SQL text.
        String ownerCondition = scope != null && scope.ownerId() != null ? "owner_id = " + scope.ownerId() : "owner_id is null";
        String inScope = scope == null ? ""
                : " and key in (" + scope.documents().stream().map(k -> "'" + k + "'").collect(Collectors.joining(", ")) + ")";
        // The rows this import manages: default content, or the part of it (or of a user's) that the scope names.
        String defaultRows = documents ? ownerCondition + inScope
                : owned ? "document_key in (select key from open5e.documents where " + ownerCondition + inScope + ")"
                : "true";
        String userRows = documents ? "owner_id is not null"
                : owned ? "document_key in (select key from open5e.documents where owner_id is not null)"
                : "false";

        // Without a scope, a user's rows are never written, even when upstream has the same key. With one, nothing is
        // quietly skipped: a key that belongs to anything else is refused below.
        Set<String> userKeys = scope != null ? new TreeSet<>() : new TreeSet<>(jdbc.queryForList(
                "select key from open5e." + table + " where " + userRows, String.class));
        int currentDefaults = jdbc.queryForObject(
                "select count(*) from open5e." + table + " where " + defaultRows, Integer.class);

        Set<String> unknownFields = new TreeSet<>();
        Set<String> skipped = new TreeSet<>();
        Set<String> upstreamKeys = new LinkedHashSet<>();
        List<Map<String, String>> upserts = new ArrayList<>();
        for (ObjectNode row : rows) {
            Map<String, String> values = toColumns(columnTypes, row, unknownFields);
            if (documents && scope != null && scope.ownerId() != null) {
                values.put("owner_id", String.valueOf(scope.ownerId())); // upstream has no owners; these are one user's
            }
            String key = values.get("key");
            if (key == null || key.isBlank()) {
                throw new IllegalStateException(table + ": upstream row without a key: " + row);
            }
            if (!upstreamKeys.add(key)) {
                throw new IllegalStateException(table + ": upstream has key '" + key + "' twice");
            }
            if (userKeys.contains(key)) {
                skipped.add(key);
            } else {
                upserts.add(values);
            }
        }
        if (scope != null) {
            // Rows limited to some documents must not quietly fail to replace rows that belong to other content.
            Set<String> own = new TreeSet<>(jdbc.queryForList("select key from open5e." + table + " where " + defaultRows,
                    String.class));
            List<String> clashes = jdbc.queryForList("select key from open5e." + table + " where key = any(?)", String.class,
                    (Object) upstreamKeys.toArray(String[]::new)).stream()
                    .filter(key -> !own.contains(key) && !userKeys.contains(key)).toList();
            if (!clashes.isEmpty()) {
                throw new IllegalStateException(table + ": " + clashes + " already belong to content outside "
                        + scope.documents() + ". Give them other keys");
            }
        }

        int stale = jdbc.queryForObject("select count(*) from open5e." + table + " where " + defaultRows
                + " and not key = any(?)", Integer.class, (Object) upstreamKeys.toArray(String[]::new));
        if (!allowLargeDeletions && currentDefaults >= SMALL_TABLE && stale > currentDefaults * MAX_DELETED_SHARE) {
            throw new IllegalStateException("%s: upstream is missing %d of %d rows; not deleting that many. If that's "
                    .formatted(table, stale, currentDefaults) + "really intended, run again allowing large deletions.");
        }

        int inserted = 0;
        int updated = 0;
        if (!upserts.isEmpty()) {
            List<String> columns = List.copyOf(upserts.getFirst().keySet());
            String sql = upsertSql(table, columns, columnTypes, defaultRows);
            for (Map<String, String> values : upserts) {
                List<Boolean> result = jdbc.queryForList(sql, Boolean.class,
                        columns.stream().map(values::get).toArray());
                if (!result.isEmpty()) {
                    if (result.getFirst()) {
                        inserted++;
                    } else {
                        updated++;
                    }
                }
            }
        }
        int deleted = jdbc.update("delete from open5e." + table + " where " + defaultRows + " and not key = any(?)",
                (Object) upstreamKeys.toArray(String[]::new));

        return new ImportReport.Table(table, rows.size(), inserted, updated, upserts.size() - inserted - updated,
                deleted, skipped, unknownFields);
    }

    /**
     * Inserts the row, or replaces the existing default row with the same key if anything differs. Returns
     * {@code true} for an insert, {@code false} for an update, and no row if nothing changed (or the existing row
     * isn't default content).
     */
    private static String upsertSql(String table, List<String> columns, Map<String, String> columnTypes,
                                    String defaultRows) {
        String names = columns.stream().map(DefaultContentImporter::quote).collect(Collectors.joining(", "));
        String values = columns.stream().map(c -> "cast(? as " + columnTypes.get(c) + ")").collect(Collectors.joining(", "));
        List<String> updatable = columns.stream().filter(c -> !c.equals("key")).toList();
        String set = updatable.stream().map(c -> quote(c) + " = excluded." + quote(c)).collect(Collectors.joining(", "));
        String current = updatable.stream().map(c -> "t." + quote(c)).collect(Collectors.joining(", "));
        String incoming = updatable.stream().map(c -> "excluded." + quote(c)).collect(Collectors.joining(", "));
        return "insert into open5e." + table + " as t (" + names + ") values (" + values + ")"
                + " on conflict (key) do update set " + set
                + " where (" + current + ") is distinct from (" + incoming + ")"
                + " and t.key in (select key from open5e." + table + " where " + defaultRows + ")"
                + " returning (xmax = 0)";
    }

    /** Column name → SQL type, in table order. */
    Map<String, String> columnTypes(String table) {
        Map<String, String> types = new LinkedHashMap<>();
        jdbc.query("""
                select column_name, data_type from information_schema.columns
                where table_schema = 'open5e' and table_name = ? order by ordinal_position""",
                rs -> {
                    types.put(rs.getString("column_name"), rs.getString("data_type"));
                }, table);
        return types;
    }

    private static String text(JsonNode value, String columnType, String field) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (columnType.equals("jsonb")) {
            return ApiUrls.rewrite(value).toString();
        }
        if (value.isContainer()) {
            throw new IllegalStateException("Field '" + field + "' is " + value.getNodeType() + " but its column is " + columnType);
        }
        return value.asString();
    }

    private static String snakeCase(String name) {
        return CAMEL.matcher(name).replaceAll("$1_$2").toLowerCase();
    }

    private static String quote(String column) {
        return '"' + column + '"';
    }
}
