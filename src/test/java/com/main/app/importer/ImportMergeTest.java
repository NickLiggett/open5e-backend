package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * How an import merges upstream rows, using {@code creaturetypes} (14 default rows) as the example. The upstream rows
 * are the table's current default content in the API's shape, then changed per test.
 * <p>
 * Every merge here runs in a transaction that is rolled back, so default content is never really changed.
 */
@SpringBootTest
class ImportMergeTest {

    static final String TABLE = "creaturetypes";
    static final String USER_DOCUMENT = "zz-test-import-homebrew";

    @Autowired
    private DefaultContentImporter importer;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void anUnchangedSourceChangesNothing() {
        rolledBack(() -> {
            ImportReport.Table report = merge(upstream(), false);

            assertThat(report.fetched()).isEqualTo(14);
            assertThat(report.unchanged()).isEqualTo(14);
            assertThat(report.inserted() + report.updated() + report.deleted()).isZero();
        });
    }

    @Test
    void insertsUpdatesAndDeletes() {
        rolledBack(() -> {
            List<ObjectNode> rows = upstream();
            ObjectNode renamed = find(rows, "aberration").put("name", "Aberration (revised)");
            rows.remove(find(rows, "ooze"));
            rows.add(jsonMapper.createObjectNode().put("key", "zz-test-new-type").put("name", "New Type")
                    .put("document", "srd-2024"));

            ImportReport.Table report = merge(rows, false);

            assertThat(report).extracting("inserted", "updated", "deleted", "unchanged").containsExactly(1, 1, 1, 12);
            assertThat(name("aberration")).isEqualTo(renamed.get("name").asString());
            assertThat(count("key = 'ooze'")).isZero();
            assertThat(jdbc.queryForObject("select document_key from open5e.creaturetypes where key = 'zz-test-new-type'",
                    String.class)).isEqualTo("srd-2024");
        });
    }

    @Test
    void neverTouchesUserContent() {
        rolledBack(() -> {
            createUserDocument();
            jdbc.update("insert into open5e.creaturetypes (key, name, document_key) values (?, 'Mine', ?)",
                    USER_DOCUMENT + "_mine", USER_DOCUMENT);

            List<ObjectNode> rows = upstream();
            rows.add(jsonMapper.createObjectNode().put("key", USER_DOCUMENT + "_mine").put("name", "Upstream's")
                    .put("document", "srd-2024"));
            ImportReport.Table report = merge(rows, false);

            assertThat(report.skippedUserKeys()).containsExactly(USER_DOCUMENT + "_mine");
            assertThat(name(USER_DOCUMENT + "_mine")).isEqualTo("Mine"); // not overwritten, not deleted as stale
        });
    }

    @Test
    void userCopiesOfDeletedRowsKeepTheirData() {
        rolledBack(() -> {
            createUserDocument();
            jdbc.update("insert into open5e.creaturetypes (key, name, document_key, derived_from) values (?, 'My Ooze', ?, 'ooze')",
                    USER_DOCUMENT + "_ooze", USER_DOCUMENT);

            List<ObjectNode> rows = upstream();
            rows.remove(find(rows, "ooze"));
            merge(rows, false);

            assertThat(count("key = 'ooze'")).isZero();
            assertThat(jdbc.queryForMap("select name, derived_from from open5e.creaturetypes where key = ?",
                    USER_DOCUMENT + "_ooze")).containsEntry("name", "My Ooze").containsEntry("derived_from", null);
        });
    }

    @Test
    void refusesToDeleteMostOfATableUnlessAllowed() {
        rolledBack(() -> {
            List<ObjectNode> twoRows = upstream().subList(0, 2);

            assertThatThrownBy(() -> merge(new ArrayList<>(twoRows), false))
                    .hasMessageContaining("upstream is missing 12 of 14 rows");
            assertThat(count("true")).isEqualTo(14);

            assertThat(merge(new ArrayList<>(twoRows), true).deleted()).isEqualTo(12);
        });
    }

    @Test
    void reportsFieldsWithNoColumn() {
        rolledBack(() -> {
            List<ObjectNode> rows = upstream();
            rows.getFirst().put("brand_new_field", "?");

            assertThat(merge(rows, false).unknownFields()).containsExactly("brand_new_field");
        });
    }

    @Test
    void aDryRunChangesNothing() {
        List<ObjectNode> rows = upstream();
        find(rows, "aberration").put("name", "Dry Run");
        rows.remove(find(rows, "ooze"));

        ImportReport report = importer.run(source(rows), List.of(TABLE), false, false);

        assertThat(report.applied()).isFalse();
        assertThat(report.changes()).isEqualTo(2);
        assertThat(name("aberration")).isEqualTo("Aberration");
        assertThat(count("key = 'ooze'")).isOne();
    }

    /** The test JVM's time zone is whatever the machine's is; the result must be the same everywhere. */
    @Test
    void readsTimestampsWithoutATimeZoneAsUtc() {
        rolledBack(() -> {
            List<ObjectNode> documents = new ArrayList<>();
            for (String json : jdbc.queryForList("""
                    select (to_jsonb(d) - 'owner_id' || jsonb_build_object('publication_date', '2024-01-01T00:00:00'))::text
                    from open5e.documents d where owner_id is null""", String.class)) {
                documents.add((ObjectNode) jsonMapper.readTree(json));
            }

            importer.merge(Map.of("documents", documents), false);

            assertThat(jdbc.queryForObject("""
                    select publication_date = timestamptz '2024-01-01 00:00:00+00' from open5e.documents
                    where key = 'srd-2024'""", Boolean.class)).isTrue();
        });
    }

    @Test
    void mergeRefusesToRunOutsideATransaction() {
        assertThatThrownBy(() -> importer.merge(Map.of(TABLE, upstream()), false))
                .hasMessageContaining("transaction");
    }

    /** The table's default rows, shaped like the API's (document as a key). */
    private List<ObjectNode> upstream() {
        List<ObjectNode> rows = new ArrayList<>();
        for (String json : jdbc.queryForList("""
                select jsonb_build_object('key', key, 'name', name, 'descriptions', descriptions, 'document', document_key)::text
                from open5e.creaturetypes where document_key in (select key from open5e.documents where owner_id is null)
                order by key""", String.class)) {
            rows.add((ObjectNode) jsonMapper.readTree(json));
        }
        return rows;
    }

    private ImportReport.Table merge(List<ObjectNode> rows, boolean allowLargeDeletions) {
        return importer.merge(Map.of(TABLE, rows), allowLargeDeletions).getFirst();
    }

    private void createUserDocument() {
        jdbc.update("insert into open5e.users (username) values ('zz-test-importer')");
        long user = jdbc.queryForObject("select id from open5e.users where username = 'zz-test-importer'", Long.class);
        jdbc.update("insert into open5e.documents (key, name, owner_id) values (?, 'Import test', ?)", USER_DOCUMENT, user);
    }

    private static ObjectNode find(List<ObjectNode> rows, String key) {
        return rows.stream().filter(r -> r.get("key").asString().equals(key)).findFirst().orElseThrow();
    }

    private String name(String key) {
        return jdbc.queryForObject("select name from open5e.creaturetypes where key = ?", String.class, key);
    }

    private int count(String condition) {
        return jdbc.queryForObject("select count(*) from open5e.creaturetypes where " + condition
                + " and document_key in (select key from open5e.documents where owner_id is null)", Integer.class);
    }

    private static DefaultContentSource source(List<ObjectNode> rows) {
        return new DefaultContentSource() {
            @Override
            public List<ObjectNode> fetch(String endpoint) {
                return rows;
            }

            @Override
            public String describe() {
                return "test rows";
            }
        };
    }

    private void rolledBack(Runnable test) {
        transactions.executeWithoutResult(status -> {
            status.setRollbackOnly();
            test.run();
        });
    }
}
