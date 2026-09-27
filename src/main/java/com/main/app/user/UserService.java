package com.main.app.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * User rows. Uses plain JDBC rather than JPA because it runs while the visibility filter is being resolved, inside a
 * Hibernate query.
 */
@Service
public class UserService {

    private final JdbcTemplate jdbc;

    public UserService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the user with this username, creating it first if needed. Safe to call concurrently. */
    public User findOrCreate(String username) {
        return find(username).orElseGet(() -> {
            // Only insert when missing: every insert attempt uses up an id, even when the conflict clause skips it.
            jdbc.update("insert into open5e.users (username) values (?) on conflict (username) do nothing", username);
            return find(username).orElseThrow();
        });
    }

    private Optional<User> find(String username) {
        return jdbc.query("select id, username from open5e.users where username = ?",
                (rs, row) -> new User(rs.getLong("id"), rs.getString("username")), username).stream().findFirst();
    }
}
