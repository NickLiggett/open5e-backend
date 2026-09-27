package com.main.app.ownership;

import com.main.app.user.DevCurrentUser;
import com.main.app.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Outside the {@code dev} profile nobody is signed in: only default content is visible, and the {@code X-User} header
 * is ignored.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnonymousVisibilityTest {

    static final String OWNER = VisibilityTest.PREFIX + "anon-owner";
    static final String DOCUMENT = VisibilityTest.PREFIX + "anon-homebrew";
    static final String CREATURE = DOCUMENT + "_secret";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void createHomebrew() {
        cleanUp();
        long owner = userService.findOrCreate(OWNER).id();
        jdbc.update("insert into open5e.documents (key, name, owner_id) values (?, 'Anon Homebrew', ?)", DOCUMENT, owner);
        jdbc.update("insert into open5e.creatures (key, name, document_key) values (?, 'Secret', ?)", CREATURE, DOCUMENT);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from open5e.creatures where document_key = ?", DOCUMENT);
        jdbc.update("delete from open5e.documents where key = ?", DOCUMENT);
        jdbc.update("delete from open5e.users where username = ?", OWNER);
    }

    @Test
    void seesOnlyDefaultContent() throws Exception {
        mvc.perform(get("/api/creatures/" + VisibilityTest.DEFAULT_CREATURE)).andExpect(status().isOk());
        mvc.perform(get("/api/creatures/" + CREATURE)).andExpect(status().isNotFound());
    }

    @Test
    void ignoresTheDevHeader() throws Exception {
        mvc.perform(get("/api/creatures/" + CREATURE).header(DevCurrentUser.HEADER, OWNER))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/me").header(DevCurrentUser.HEADER, OWNER))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cannotWrite() throws Exception {
        mvc.perform(post("/api/creatures").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Anon\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/creatures/" + VisibilityTest.DEFAULT_CREATURE + "/copy"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/documents").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Anon\"}"))
                .andExpect(status().isUnauthorized());
    }
}
