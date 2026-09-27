package com.main.app.ownership;

/**
 * The visibility rule, as a Hibernate filter that is enabled in every session (see
 * {@link com.main.app.document.Document}). A user can see a document if it has no owner (default content), they own
 * it, or they are a member of it; they can see a resource if they can see its document.
 * <p>
 * The filter applies to every JPA query and to loads by key, so repositories get it without doing anything. It does
 * not apply to native SQL: don't query resource tables with native queries in application code.
 */
public final class Visibility {

    public static final String FILTER = "visibility";
    public static final String USER_ID = "userId";

    /** The {@code userId} used when nobody is signed in. No user has it, so only default content is visible. */
    public static final long ANONYMOUS = -1;

    public static final String DOCUMENT_CONDITION = """
            ({alias}.owner_id is null
             or {alias}.owner_id = :userId
             or exists (select 1 from open5e.document_members m where m.document_key = {alias}.key and m.user_id = :userId))""";

    public static final String RESOURCE_CONDITION = """
            {alias}.document_key in (
                select d.key from open5e.documents d
                where d.owner_id is null
                   or d.owner_id = :userId
                   or exists (select 1 from open5e.document_members m where m.document_key = d.key and m.user_id = :userId))""";

    private Visibility() {
    }
}
