package com.main.app.ownership;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The database refuses changes to default content even if the app tried (V5 triggers), unless a transaction
 * explicitly allows them. Every change here is rolled back.
 */
@SpringBootTest
class DefaultContentProtectionTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @Test
    void refusesChangesToDefaultResources() {
        assertRefused(jdbc -> jdbc.update("update open5e.creatures set name = 'Changed' where key = 'a5e-mm_aboleth'"));
        assertRefused(jdbc -> jdbc.update("delete from open5e.spells where key = (select min(key) from open5e.spells)"));
        assertRefused(jdbc -> jdbc.update(
                "insert into open5e.creatures (key, name, document_key) values ('a5e-mm_sneaky', 'Sneaky', 'a5e-mm')"));
    }

    @Test
    void refusesChangesToDefaultDocuments() {
        assertRefused(jdbc -> jdbc.update("update open5e.documents set name = 'Changed' where key = 'srd-2024'"));
        assertRefused(jdbc -> jdbc.update("delete from open5e.documents where key = 'srd-2024'"));
        assertRefused(jdbc -> jdbc.update("insert into open5e.documents (key, name) values ('zz-test-fake-source', 'Fake')"));
    }

    @Test
    void allowsChangesWhenATransactionOptsIn() {
        rolledBack(jdbc -> {
            jdbc.execute("set local open5e.allow_default_content_changes = 'on'");
            jdbc.update("update open5e.creatures set name = 'Changed' where key = 'a5e-mm_aboleth'");
            assertThat(jdbc.queryForObject("select name from open5e.creatures where key = 'a5e-mm_aboleth'", String.class))
                    .isEqualTo("Changed");
        });
        assertThat(jdbc.queryForObject("select name from open5e.creatures where key = 'a5e-mm_aboleth'", String.class))
                .isEqualTo("Aboleth");
    }

    private void assertRefused(Consumer<JdbcTemplate> change) {
        assertThatThrownBy(() -> rolledBack(change))
                .isInstanceOf(DataAccessException.class)
                .rootCause().hasMessageContaining("can't be changed");
    }

    private void rolledBack(Consumer<JdbcTemplate> change) {
        transactions.executeWithoutResult(status -> {
            status.setRollbackOnly();
            change.accept(jdbc);
        });
    }
}
