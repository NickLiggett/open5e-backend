package com.main.app.user;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outside the {@code dev} profile, nobody is signed in yet, so every request sees only default content. Replaced by
 * token-based sign-in in phase 5.
 */
@Component
@Profile("!dev")
public class AnonymousCurrentUser implements CurrentUser {

    @Override
    public Optional<Long> id() {
        return Optional.empty();
    }

    @Override
    public Optional<String> username() {
        return Optional.empty();
    }
}
