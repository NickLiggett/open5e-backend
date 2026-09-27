package com.main.app.user;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

/**
 * Every profile except {@code dev}: the user is whoever the request's bearer token says, a JWT from the OpenID
 * Connect issuer (see {@link SecurityConfig}). Requests without a token are anonymous and see only default content.
 * <p>
 * The user row is found by the token's issuer and subject, and created on first sign-in with a username taken from
 * {@code preferred_username}. Like {@link DevCurrentUser}, it's resolved once at the start of the request, outside
 * any transaction.
 */
@Component
@Profile("!dev")
public class JwtCurrentUser implements CurrentUser {

    private static final String REQUEST_ATTRIBUTE = JwtCurrentUser.class.getName() + ".user";

    private final UserService userService;

    public JwtCurrentUser(UserService userService) {
        this.userService = userService;
    }

    /** Called by the security filter chain once the bearer token, if any, has been verified. */
    void resolve(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            String subject = jwt.getClaimAsString("iss") + "|" + jwt.getSubject();
            request.setAttribute(REQUEST_ATTRIBUTE, userService.findOrCreateFromToken(subject,
                    jwt.getClaimAsString("preferred_username"), jwt.getClaimAsString("name")));
        }
    }

    @Override
    public Optional<Long> id() {
        return user().map(User::id);
    }

    @Override
    public Optional<String> username() {
        return user().map(User::username);
    }

    private static Optional<User> user() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty(); // not in a web request
        }
        return Optional.ofNullable((User) attributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST));
    }
}
