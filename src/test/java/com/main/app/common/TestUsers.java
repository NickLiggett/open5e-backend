package com.main.app.common;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test users are named {@value #PREFIX}*. Tests that create content through the API commit it (so each request gets
 * its own Hibernate session, as in production) and call {@link #cleanUp} before and after.
 */
public final class TestUsers {

    public static final String PREFIX = "zz-test-";

    private TestUsers() {
    }

    /** Deletes the test users and everything in their documents, in every resource table. */
    public static void cleanUp(JdbcTemplate jdbc) {
        String testDocuments = """
                select d.key from open5e.documents d join open5e.users u on u.id = d.owner_id
                where u.username like '%s%%'""".formatted(PREFIX);
        for (String table : jdbc.queryForList("""
                select table_name from information_schema.columns
                where table_schema = 'open5e' and column_name = 'document_key' and table_name <> 'document_members'""",
                String.class)) {
            jdbc.update("delete from open5e." + table + " where document_key in (" + testDocuments + ")");
        }
        jdbc.update("delete from open5e.documents where key in (" + testDocuments + ")");
        jdbc.update("delete from open5e.users where username like ?", PREFIX + "%");
    }
}
