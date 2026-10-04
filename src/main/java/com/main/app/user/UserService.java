package com.main.app.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * User rows. Users are resolved at the start of each request, before any transaction or Hibernate session, so this
 * uses plain JDBC with auto-commit.
 */
@Service
public class UserService {

    private static final int MAX_USERNAME_SUFFIX = 100;

    private final JdbcTemplate jdbc;

    public UserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the user with this username, creating it first if needed. Safe to call concurrently. */
    public User findOrCreate(String username) {
        return find("username", username).orElseGet(() -> {
            // Only insert when missing: every insert attempt uses up an id, even when the conflict clause skips it.
            jdbc.update("insert into open5e.users (username) values (?) on conflict (username) do nothing", username);
            return find("username", username).orElseThrow();
        });
    }

    /**
     * Returns the user signed in with this identity provider subject, creating it on first sign-in. The username is
     * the token's preferred username, made to fit the username rules; if another user has it, {@code -2}, {@code -3},
     * … is added. Safe to call concurrently.
     *
     * The token's email address is remembered only when the sign-in service says the person has verified it; that is
     * what lets them accept the invitations sent to it (see {@link #recordEmail}).
     *
     * @param idpSubject    the token's issuer and subject, which together identify the person
     * @param email         the token's {@code email} claim, if it has one
     * @param emailVerified the token's {@code email_verified} claim, if it has one
     */
    public User findOrCreateFromToken(String idpSubject, String preferredUsername, String displayName, String email,
                                      Boolean emailVerified) {
        User user = findOrCreateFromToken(idpSubject, preferredUsername, displayName);
        recordEmail(user, email, emailVerified);
        return user;
    }

    private User findOrCreateFromToken(String idpSubject, String preferredUsername, String displayName) {
        Optional<User> existing = find("idp_subject", idpSubject);
        if (existing.isPresent()) {
            return existing.get();
        }
        String base = usernameFrom(preferredUsername);
        for (int suffix = 1; suffix <= MAX_USERNAME_SUFFIX; suffix++) {
            String username = suffix == 1 ? base : base + "-" + suffix;
            int inserted = jdbc.update("""
                    insert into open5e.users (username, idp_subject, display_name) values (?, ?, ?)
                    on conflict do nothing""", username, idpSubject, displayName);
            Optional<User> user = find("idp_subject", idpSubject);
            if (inserted == 1 || user.isPresent()) {
                return user.orElseThrow(); // ours, or created by a concurrent request for the same person
            }
        }
        throw new IllegalStateException("No free username for '" + base + "'");
    }

    /**
     * Keeps the user's verified address in step with the token: set when verified, cleared when the token says it
     * isn't, left alone when the token doesn't say. When the address is new to the user, the invitations sent to it
     * are accepted: each becomes a membership (never lowering a role they have), and all of them are deleted.
     */
    private void recordEmail(User user, String email, Boolean emailVerified) {
        if (emailVerified == null) {
            return;
        }
        String verified = emailVerified ? Emails.normalize(email) : null;
        if (emailVerified && verified == null) {
            return; // verified, but not an address we can compare
        }
        int changed = jdbc.update("update open5e.users set verified_email = ? where id = ? and verified_email is distinct from ?",
                verified, user.id(), verified);
        if (changed == 1 && verified != null) {
            acceptInvitations(user.id(), verified);
        }
    }

    private void acceptInvitations(long userId, String email) {
        jdbc.update("""
                insert into open5e.document_members (document_key, user_id, role)
                select i.document_key, ?, i.role from open5e.document_invitations i
                join open5e.documents d on d.key = i.document_key
                where i.email = ? and i.expires_at > now() and d.owner_id is distinct from ?
                on conflict (document_key, user_id) do update set role =
                    case when open5e.document_members.role = 'EDITOR' or excluded.role = 'EDITOR' then 'EDITOR' else 'VIEWER' end""",
                userId, email, userId);
        jdbc.update("delete from open5e.document_invitations where email = ?", email);
    }

    private Optional<User> find(String column, String value) {
        return jdbc.query("select id, username from open5e.users where " + column + " = ?",
                (rs, row) -> new User(rs.getLong("id"), rs.getString("username")), value).stream().findFirst();
    }

    /** Lowercase letters, digits and hyphens, like dev usernames, up to 28 characters (room for a suffix). */
    static String usernameFrom(String preferredUsername) {
        String username = preferredUsername == null ? "" : preferredUsername.toLowerCase(Locale.ROOT)
                .replaceAll("@.*$", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (username.length() > 28) {
            username = username.substring(0, 28).replaceAll("-+$", "");
        }
        return username.isEmpty() ? "user" : username;
    }
}
