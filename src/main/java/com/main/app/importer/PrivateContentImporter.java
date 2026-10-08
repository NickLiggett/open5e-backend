package com.main.app.importer;

import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the private content ({@link CustomContent#privateContent()}): each file's documents are made, kept up to date
 * and pruned as documents <em>owned by the user the file names</em>. That makes them that user's own: only they see them
 * until they share them on the Sharing page, like any of their documents, and then the people they share with see the
 * content too (search, the Species page, the Players page).
 * <p>
 * Only the documents a file defines are ever touched. The owner's other documents, other users' content and all default
 * content are left exactly as they are, and a key that already belongs to any of them is refused.
 */
@Lazy
@Service
public class PrivateContentImporter {

    private final CustomContent custom;
    private final DefaultContentImporter importer;
    private final JdbcTemplate jdbc;

    public PrivateContentImporter(CustomContent custom, DefaultContentImporter importer, JdbcTemplate jdbc) {
        this.custom = custom;
        this.importer = importer;
        this.jdbc = jdbc;
    }

    /** Whether there is any private content to apply. */
    public boolean any() {
        return !custom.privateContent().isEmpty();
    }

    /**
     * Applies each private file in its own transaction.
     *
     * @param apply false for a dry run: report what would change
     * @throws IllegalStateException if a file's owner has no account here yet, or a key clashes with other content
     */
    public List<ImportReport> run(boolean apply, boolean allowLargeDeletions) {
        List<ImportReport> reports = new ArrayList<>();
        for (CustomContent.Private one : custom.privateContent()) {
            ContentSet content = one.content();
            reports.add(importer.run(content.asSource("private content of " + one.owner() + ", " + one.file()),
                    content.tables(), apply, allowLargeDeletions,
                    DefaultContentImporter.Scope.ownedBy(ownerId(one), content.documentKeys())));
        }
        return reports;
    }

    private long ownerId(CustomContent.Private one) {
        return jdbc.queryForList("select id from open5e.users where username = ?", Long.class, one.owner()).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Private content " + one.file() + " belongs to '" + one.owner()
                        + "', who has no account here yet. Sign in to the app once as them (or fix the \"owner\" in the file), "
                        + "then run the import again"));
    }
}
