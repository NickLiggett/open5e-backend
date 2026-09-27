package com.main.app.user;

import java.util.Optional;

/**
 * Who is making the current request. Services and the visibility filter ask this rather than reading requests or
 * tokens themselves, so the way users sign in can change without touching them.
 * <ul>
 *     <li>{@code dev} profile: {@link DevCurrentUser}, chosen by the {@code X-User} header</li>
 *     <li>any other profile: {@link AnonymousCurrentUser} until real sign-in exists (see docs/PLAN.md, phase 5)</li>
 * </ul>
 */
public interface CurrentUser {

    /** The current user's id, or empty if nobody is signed in (only default content is visible). */
    Optional<Long> id();

    /** The current user's username, or empty if nobody is signed in. */
    Optional<String> username();
}
