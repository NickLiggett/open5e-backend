package com.main.app.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Local development only: the user is named by the {@code X-User} request header, or {@value #DEFAULT_USERNAME} if
 * there is none, and is created on first use. Anyone can claim to be anyone, so this must never run outside
 * {@code dev}.
 * <p>
 * The user is resolved by this servlet filter at the start of each request, outside any transaction (creating a user
 * is a write, and request handling may run in read-only transactions). {@link #id()} then only reads it back.
 */
@Component
@Profile("dev")
public class DevCurrentUser extends OncePerRequestFilter implements CurrentUser {

    public static final String HEADER = "X-User";
    public static final String DEFAULT_USERNAME = "dev";

    private static final Logger log = LoggerFactory.getLogger(DevCurrentUser.class);
    private static final Pattern USERNAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,31}");
    private static final String REQUEST_ATTRIBUTE = DevCurrentUser.class.getName() + ".user";

    private final UserService userService;

    public DevCurrentUser(UserService userService) {
        this.userService = userService;
        log.warn("Dev sign-in is active: requests choose their user with the {} header. Never use the dev profile "
                + "outside local development.", HEADER);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        String username = header == null || header.isBlank() ? DEFAULT_USERNAME : header.trim().toLowerCase();
        if (!USERNAME.matcher(username).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    HEADER + " must be 1-32 lowercase letters, digits or hyphens");
            return;
        }
        request.setAttribute(REQUEST_ATTRIBUTE, userService.findOrCreate(username));
        chain.doFilter(request, response);
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
