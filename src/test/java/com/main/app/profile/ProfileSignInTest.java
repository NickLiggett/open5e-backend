package com.main.app.profile;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who can use a user's settings, avatar and tracker state with real sign-in (bearer tokens, the default profile): only
 * the signed-in user can change theirs or read theirs, but anyone can see an avatar, because a browser's {@code <img>}
 * has no token to send. Tokens are built by Spring Security's test support, as in the sign-in tests.
 */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/no-keys-in-tests")
@AutoConfigureMockMvc
class ProfileSignInTest {

    static final String ISSUER = "http://localhost:8180/realms/open5e";
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};

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
    void nobodySignedInCanReadOrChangeSettingsOrTracker() throws Exception {
        mvc.perform(get("/api/me/settings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/tracker")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/settings").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/tracker").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/me/encounters")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/encounters").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void nobodySignedInCanChangeAnAvatar() throws Exception {
        mvc.perform(put("/api/me/avatar").contentType("image/png").content(PNG)).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/me/avatar")).andExpect(status().isUnauthorized());
    }

    @Test
    void aSignedInUserKeepsTheirOwnThings() throws Exception {
        mvc.perform(put("/api/me/settings").with(token("sub-a", TestUsers.PREFIX + "a"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mode\": \"dark\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/me/tracker").with(token("sub-a", TestUsers.PREFIX + "a"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"round\": 4}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me/settings").with(token("sub-a", TestUsers.PREFIX + "a")))
                .andExpect(jsonPath("$.mode").value("dark"));
        mvc.perform(get("/api/me/tracker").with(token("sub-a", TestUsers.PREFIX + "a")))
                .andExpect(jsonPath("$.round").value(4));
        mvc.perform(get("/api/me/settings").with(token("sub-b", TestUsers.PREFIX + "b")))
                .andExpect(jsonPath("$.mode").doesNotExist());
        mvc.perform(get("/api/me/tracker").with(token("sub-b", TestUsers.PREFIX + "b")))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void savedEncountersAreOnlyTheSignedInUsers() throws Exception {
        mvc.perform(put("/api/me/encounters").with(token("sub-a", TestUsers.PREFIX + "a"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"encounters\": [{\"id\": 1, \"name\": \"Ambush\"}]}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/me/encounters").with(token("sub-a", TestUsers.PREFIX + "a")))
                .andExpect(jsonPath("$.encounters[0].name").value("Ambush"));
        mvc.perform(get("/api/me/encounters").with(token("sub-b", TestUsers.PREFIX + "b")))
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void anyoneCanSeeAnAvatarWithoutSigningIn() throws Exception {
        mvc.perform(put("/api/me/avatar").with(token("sub-a", TestUsers.PREFIX + "a")).contentType("image/png").content(PNG))
                .andExpect(status().isOk());

        mvc.perform(get("/api/users/" + TestUsers.PREFIX + "a/avatar")).andExpect(status().isOk());
        mvc.perform(get("/api/users/" + TestUsers.PREFIX + "b/avatar")).andExpect(status().isNotFound());
    }

    private static RequestPostProcessor token(String subject, String preferredUsername) {
        return jwt().jwt(jwt -> jwt.subject(subject).issuer(ISSUER).claim("preferred_username", preferredUsername));
    }
}
