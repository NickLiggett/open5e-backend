package com.main.app.user;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * How requests are signed in. The API is stateless (no sessions or cookies, so no CSRF tokens); what each user may
 * see and change is decided by the visibility filter and {@code DocumentAccess}, not here.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class SecurityConfig {

    /** {@code dev}: no tokens; {@link DevCurrentUser} takes the user from the {@code X-User} header. */
    @Bean
    @Profile("dev")
    SecurityFilterChain devSecurity(HttpSecurity http) throws Exception {
        return stateless(http)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }

    /**
     * Every other profile: bearer tokens (JWTs) from the configured OpenID Connect issuer. Reading is open to
     * anonymous requests (they see default content); everything else needs a valid token. An invalid or expired
     * token is always a {@code 401}, even for reads.
     */
    @Bean
    @Profile("!dev")
    SecurityFilterChain tokenSecurity(HttpSecurity http, JwtCurrentUser currentUser) throws Exception {
        return stateless(http)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
                .addFilterAfter(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                    FilterChain chain) throws ServletException, IOException {
                        currentUser.resolve(request);
                        chain.doFilter(request, response);
                    }
                }, BearerTokenAuthenticationFilter.class)
                .build();
    }

    private static HttpSecurity stateless(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
