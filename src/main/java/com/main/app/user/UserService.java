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
     * @param idpSubject the token's issuer and subject, which together identify the person
     */
    public User findOrCreateFromToken(String idpSubject, String preferredUsername, String displayName) {
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
