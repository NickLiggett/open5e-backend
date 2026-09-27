package com.main.app.user;

import com.jayway.jsonpath.JsonPath;
import com.main.app.common.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sign-in with bearer tokens (the default profile). Tokens here are built by Spring Security's test support, which
 * skips signature checks; a real token from Keycloak goes through the same code after verification. Keycloak isn't
 * running, so the key set URL points nowhere: malformed tokens are rejected before keys are needed.
 */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/no-keys-in-tests")
@AutoConfigureMockMvc
class TokenSignInTest {

    static final String ISSUER = "http://localhost:8180/realms/open5e";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void createsTheUserOnFirstSignInAndFindsThemAfter() throws Exception {
        String first = mvc.perform(get("/api/me").with(token("sub-dm", TestUsers.PREFIX + "dm")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TestUsers.PREFIX + "dm"))
                .andReturn().getResponse().getContentAsString();
        String again = mvc.perform(get("/api/me").with(token("sub-dm", TestUsers.PREFIX + "renamed-in-keycloak")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(again).isEqualTo(first); // same person (issuer + subject), username kept
        assertThat(jdbc.queryForObject("select idp_subject from open5e.users where username = ?", String.class,
                TestUsers.PREFIX + "dm")).isEqualTo(ISSUER + "|sub-dm");
    }

    @Test
    void givesTakenUsernamesASuffix() throws Exception {
        userService.findOrCreate(TestUsers.PREFIX + "taken"); // e.g. a dev-profile user

        mvc.perform(get("/api/me").with(token("sub-a", TestUsers.PREFIX + "taken")))
                .andExpect(jsonPath("$.username").value(TestUsers.PREFIX + "taken-2"));
        mvc.perform(get("/api/me").with(token("sub-b", TestUsers.PREFIX + "Taken")))
                .andExpect(jsonPath("$.username").value(TestUsers.PREFIX + "taken-3"));
    }

    @Test
    void signedInUsersCanWrite() throws Exception {
        String body = mvc.perform(post("/api/creatures").with(token("sub-writer", TestUsers.PREFIX + "writer"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Token Owlbear\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String key = JsonPath.read(body, "$.key");

        mvc.perform(get("/api/creatures/" + key).with(token("sub-writer", TestUsers.PREFIX + "writer")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/creatures/" + key).with(token("sub-other", TestUsers.PREFIX + "other")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/creatures/" + key)).andExpect(status().isNotFound());
    }

    @Test
    void anonymousRequestsCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/creatures/a5e-mm_aboleth")).andExpect(status().isOk());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/creatures").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Anon\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidTokensEvenForReads() throws Exception {
        mvc.perform(get("/api/creatures/a5e-mm_aboleth").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void makesPreferredUsernamesFitTheUsernameRules() {
        assertThat(UserService.usernameFrom("Jane.Doe@example.com")).isEqualTo("jane-doe");
        assertThat(UserService.usernameFrom("  Ælf Wizard!! ")).isEqualTo("lf-wizard");
        assertThat(UserService.usernameFrom(null)).isEqualTo("user");
        assertThat(UserService.usernameFrom("x".repeat(40))).hasSize(28);
    }

    private static RequestPostProcessor token(String subject, String preferredUsername) {
        return jwt().jwt(jwt -> jwt.subject(subject).issuer(ISSUER).claim("preferred_username", preferredUsername));
    }
}
