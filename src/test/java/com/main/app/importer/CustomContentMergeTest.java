package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Custom content against the real database: that files are well formed for their tables, that applying them adds only
 * their own rows, and that it can't touch anything else. Every merge runs in a transaction that is rolled back.
 * <p>
 * Both kinds are covered: default content (documents with no owner, visible to everyone) and private content (documents
 * owned by a user).
 */
@SpringBootTest
class CustomContentMergeTest {

    static final String OWNER = "zz-test-owner";

    @Autowired
    private DefaultContentImporter importer;

    @Autowired
    private DefaultContentSource source;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JsonMapper mapper;

    private Map<String, List<ObjectNode>> contentOf(ContentSet set) {
        Map<String, List<ObjectNode>> content = new LinkedHashMap<>();
        set.tables().forEach(table -> content.put(table, new ArrayList<>(set.rows(table))));
        return content;
    }

    private CustomContent defaults() {
        return ContentFixtures.content(mapper, ContentFixtures.DEFAULT);
    }

    private CustomContent.Private privateFile() {
        return ContentFixtures.content(mapper, null, ContentFixtures.PRIVATE).privateContent().getFirst();
    }

    // ----- default content

    @Test
    void theDefaultFilesAreWellFormedForTheirTables() {
        CustomContent custom = defaults();
        rolledBack(() -> {
            for (ImportReport.Table report : importer.merge(contentOf(custom.shared()), false, custom.documentKeys())) {
                assertThat(report.unknownFields()).as(report.name() + " fields with no column").isEmpty();
            }
        });
    }

    @Test
    void addsOnlyItsOwnRowsAndLeavesOpen5esAlone() {
        CustomContent custom = defaults();
        rolledBack(() -> {
            int speciesBefore = count("species", "true");
            int otherSpeciesBefore = count("species", "document_key <> 'zz-test-book'");
            int documentsBefore = count("documents", "true");

            List<ImportReport.Table> reports = importer.merge(contentOf(custom.shared()), false, custom.documentKeys());

            assertThat(report(reports, "species").inserted()).isEqualTo(2);
            assertThat(report(reports, "species").deleted()).isZero();
            assertThat(report(reports, "documents").inserted()).isEqualTo(1);
            assertThat(count("species", "true")).isEqualTo(speciesBefore + 2);
            assertThat(count("species", "document_key <> 'zz-test-book'")).isEqualTo(otherSpeciesBefore);
            assertThat(count("documents", "true")).isEqualTo(documentsBefore + 1);
            assertThat(jdbc.queryForObject("select owner_id from open5e.documents where key = 'zz-test-book'", Long.class)).isNull();
            assertThat(jdbc.queryForObject("select subspecies_of_key from open5e.species where key = 'zz-test-book_b'", String.class))
                    .isEqualTo("zz-test-book_a");
        });
    }

    @Test
    void aSecondApplicationChangesNothing() {
        CustomContent custom = defaults();
        rolledBack(() -> {
            importer.merge(contentOf(custom.shared()), false, custom.documentKeys());

            for (ImportReport.Table report : importer.merge(contentOf(custom.shared()), false, custom.documentKeys())) {
                assertThat(report.inserted() + report.updated() + report.deleted()).as(report.name()).isZero();
            }
        });
    }

    @Test
    void aChangeIsAnUpdateAndARemovalIsADeleteWithinItsDocumentOnly() {
        CustomContent custom = defaults();
        rolledBack(() -> {
            importer.merge(contentOf(custom.shared()), false, custom.documentKeys());
            Map<String, List<ObjectNode>> changed = contentOf(custom.shared());
            changed.get("species").getFirst().put("name", "Renamed");
            changed.get("species").removeLast();

            ImportReport.Table report = report(importer.merge(changed, false, custom.documentKeys()), "species");

            assertThat(report.updated()).isEqualTo(1);
            assertThat(report.deleted()).isEqualTo(1);
            assertThat(jdbc.queryForObject("select name from open5e.species where key = 'zz-test-book_a'", String.class)).isEqualTo("Renamed");
            assertThat(count("species", "document_key <> 'zz-test-book'")).isGreaterThan(50); // Open5e's are all still there
        });
    }

    @Test
    void refusesToReplaceARowThatBelongsToOtherContent() {
        CustomContent custom = defaults();
        rolledBack(() -> {
            Map<String, List<ObjectNode>> content = contentOf(custom.shared());
            content.get("species").getFirst().put("key", "srd_dwarf"); // Open5e's Dwarf

            assertThatThrownBy(() -> importer.merge(content, false, custom.documentKeys()))
                    .hasMessageContaining("species: [srd_dwarf] already belong to content outside");
        });
    }

    @Test
    void theSourceTheImporterIsGivenIsOpen5ePlusTheCustomContent() {
        assertThat(source).isInstanceOf(LayeredContentSource.class);
        assertThat(source.describe()).contains("+ custom content");
    }

    @Test
    void contentCannotBeLimitedToDocumentsInATableWithoutDocuments() {
        rolledBack(() -> {
            Map<String, List<ObjectNode>> content = Map.of("flyway_schema_history", new ArrayList<>());

            assertThatThrownBy(() -> importer.merge(content, false, Set.of("zz-test-book"))).isInstanceOf(RuntimeException.class);
        });
    }

    @Test
    void refusesDocumentKeysThatAreNotPlainCharacters() {
        rolledBack(() -> assertThatThrownBy(() -> importer.merge(Map.of(), false, Set.of("a' or '1'='1")))
                .hasMessageContaining("Not a usable document key"));
    }

    // ----- private content: documents owned by a user

    @Test
    void privateContentBecomesTheOwnersDocumentsAndNobodyElses() {
        CustomContent.Private file = privateFile();
        rolledBack(() -> {
            long owner = createUser(OWNER);

            List<ImportReport.Table> reports = importer.merge(contentOf(file.content()), false,
                    DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys()));

            assertThat(report(reports, "documents").inserted()).isEqualTo(1);
            assertThat(report(reports, "species").inserted()).isEqualTo(2);
            assertThat(jdbc.queryForObject("select owner_id from open5e.documents where key = 'zz-test-private'", Long.class)).isEqualTo(owner);
            assertThat(count("species", "document_key = 'zz-test-private'")).isEqualTo(2);
            // It is not default content: the rows of default content don't include it.
            assertThat(jdbc.queryForObject("select count(*) from open5e.species where document_key in "
                    + "(select key from open5e.documents where owner_id is null) and key like 'zz-test-private%'", Integer.class)).isZero();
        });
    }

    @Test
    void privateFilesAreWellFormedForTheirTables() {
        CustomContent.Private file = privateFile();
        rolledBack(() -> {
            long owner = createUser(OWNER);

            for (ImportReport.Table report : importer.merge(contentOf(file.content()), false,
                    DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys()))) {
                assertThat(report.unknownFields()).as(report.name() + " fields with no column").isEmpty();
            }
        });
    }

    @Test
    void applyingPrivateContentAgainChangesNothing_aChangeUpdates_aRemovalDeletes() {
        CustomContent.Private file = privateFile();
        rolledBack(() -> {
            long owner = createUser(OWNER);
            DefaultContentImporter.Scope scope = DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys());
            importer.merge(contentOf(file.content()), false, scope);

            for (ImportReport.Table report : importer.merge(contentOf(file.content()), false, scope)) {
                assertThat(report.inserted() + report.updated() + report.deleted()).as(report.name()).isZero();
            }
            Map<String, List<ObjectNode>> changed = contentOf(file.content());
            changed.get("species").getFirst().put("name", "Renamed");
            changed.get("species").removeLast();
            ImportReport.Table report = report(importer.merge(changed, false, scope), "species");

            assertThat(report.updated()).isEqualTo(1);
            assertThat(report.deleted()).isEqualTo(1);
            assertThat(count("species", "document_key <> 'zz-test-private'")).isGreaterThan(50); // everything else is as it was
        });
    }

    @Test
    void privateContentNeverTouchesTheOwnersOtherDocumentsOrAnyoneElses() {
        CustomContent.Private file = privateFile();
        rolledBack(() -> {
            long owner = createUser(OWNER);
            long other = createUser("zz-test-other");
            jdbc.update("insert into open5e.documents (key, name, owner_id) values ('zz-test-mine', 'Mine', ?)", owner);
            jdbc.update("insert into open5e.documents (key, name, owner_id) values ('zz-test-theirs', 'Theirs', ?)", other);
            jdbc.update("insert into open5e.species (key, name, document_key) values ('zz-test-mine_s', 'Mine', 'zz-test-mine')");
            jdbc.update("insert into open5e.species (key, name, document_key) values ('zz-test-theirs_s', 'Theirs', 'zz-test-theirs')");

            importer.merge(contentOf(file.content()), false, DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys()));
            importer.merge(Map.of("species", new ArrayList<>()), false, DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys()));

            assertThat(count("species", "key in ('zz-test-mine_s', 'zz-test-theirs_s')")).isEqualTo(2);
            assertThat(count("documents", "key in ('zz-test-mine', 'zz-test-theirs')")).isEqualTo(2);
        });
    }

    @Test
    void privateContentRefusesKeysThatBelongToOtherContent() {
        CustomContent.Private file = privateFile();
        rolledBack(() -> {
            long owner = createUser(OWNER);
            long other = createUser("zz-test-other");
            jdbc.update("insert into open5e.documents (key, name, owner_id) values ('zz-test-theirs', 'Theirs', ?)", other);
            jdbc.update("insert into open5e.species (key, name, document_key) values ('zz-test-private_x', 'Theirs', 'zz-test-theirs')");

            assertThatThrownBy(() -> importer.merge(contentOf(file.content()), false,
                    DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys())))
                    .hasMessageContaining("species: [zz-test-private_x] already belong to content outside");

            Map<String, List<ObjectNode>> content = contentOf(file.content());
            content.get("species").getFirst().put("key", "srd_dwarf");
            assertThatThrownBy(() -> importer.merge(content, false, DefaultContentImporter.Scope.ownedBy(owner, file.content().documentKeys())))
                    .hasMessageContaining("[srd_dwarf] already belong to content outside");
        });
    }

    @Test
    void aDryRunOfPrivateContentChangesNothing_andAMissingOwnerIsSaidInPlainWords() {
        CustomContent custom = ContentFixtures.content(mapper, null, ContentFixtures.PRIVATE);
        PrivateContentImporter privateImporter = new PrivateContentImporter(custom, importer, jdbc);

        assertThatThrownBy(() -> privateImporter.run(false, false))
                .hasMessageContaining("belongs to 'zz-test-owner', who has no account here yet")
                .hasMessageContaining("Sign in to the app once");
        assertThat(count("documents", "key = 'zz-test-private'")).isZero();
    }

    private ImportReport.Table report(List<ImportReport.Table> reports, String table) {
        return reports.stream().filter(r -> r.name().equals(table)).findFirst().orElseThrow();
    }

    private long createUser(String username) {
        jdbc.update("insert into open5e.users (username) values (?) on conflict do nothing", username);
        return jdbc.queryForObject("select id from open5e.users where username = ?", Long.class, username);
    }

    private int count(String table, String condition) {
        return jdbc.queryForObject("select count(*) from open5e." + table + " where " + condition, Integer.class);
    }

    private void rolledBack(Runnable test) {
        transactions.executeWithoutResult(status -> {
            status.setRollbackOnly();
            test.run();
        });
    }
}
