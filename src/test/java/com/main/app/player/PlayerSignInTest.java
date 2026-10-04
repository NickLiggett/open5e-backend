package com.main.app.player;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Players need a signed-in user: with bearer tokens (the default profile), anonymous requests are refused. */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/no-keys-in-tests")
@AutoConfigureMockMvc
class PlayerSignInTest {

    static final String ISSUER = "http://localhost:8180/realms/open5e";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void anonymousRequestsAreRefused() throws Exception {
        mvc.perform(get("/api/players")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/players").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"X\", \"ruleset\": \"5e-2014\"}")).andExpect(status().isUnauthorized());
    }

    @Test
    void signedInUsersKeepTheirOwn() throws Exception {
        var token = jwt().jwt(jwt -> jwt.subject("sub-pc").issuer(ISSUER).claim("preferred_username", TestUsers.PREFIX + "pc"));

        mvc.perform(post("/api/players").with(token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Token Hero\", \"ruleset\": \"5e-2024\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/players").with(token))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].owner").value(TestUsers.PREFIX + "pc"));
    }
}
