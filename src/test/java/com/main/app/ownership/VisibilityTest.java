package com.main.app.ownership;

import com.jayway.jsonpath.JsonPath;
import com.main.app.user.DevCurrentUser;
import com.main.app.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who can see a user's content, through real requests in the {@code dev} profile. A DM owns a homebrew document with
 * one creature and shares it with a player; a stranger has no access.
 * <p>
 * The test rows are committed and deleted after each test, rather than rolled back, so that each request gets its own
 * Hibernate session as it would in production.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class VisibilityTest {

    static final String PREFIX = "zz-test-";
    static final String DM = PREFIX + "dm";
    static final String PLAYER = PREFIX + "player";
    static final String STRANGER = PREFIX + "stranger";
    static final String DOCUMENT = PREFIX + "dm-homebrew";
    static final String CREATURE = DOCUMENT + "_owlbear-king";
    static final String DEFAULT_CREATURE = "a5e-mm_aboleth";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void createHomebrew() {
        cleanUp();
        long dm = userService.findOrCreate(DM).id();
        long player = userService.findOrCreate(PLAYER).id();
        userService.findOrCreate(STRANGER);

        jdbc.update("insert into open5e.documents (key, name, display_name, type, owner_id) values (?, ?, ?, 'HOMEBREW', ?)",
                DOCUMENT, "DM Homebrew", "DM Homebrew", dm);
        jdbc.update("insert into open5e.creatures (key, name, document_key, derived_from) values (?, ?, ?, ?)",
                CREATURE, "Owlbear King", DOCUMENT, DEFAULT_CREATURE);
        jdbc.update("insert into open5e.document_members (document_key, user_id, role) values (?, ?, 'VIEWER')",
                DOCUMENT, player);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from open5e.creatures where document_key like ?", PREFIX + "%");
        jdbc.update("delete from open5e.documents where key like ?", PREFIX + "%");
        jdbc.update("delete from open5e.users where username like ?", PREFIX + "%");
    }

    @Test
    void ownerSeesTheirCreature() throws Exception {
        mvc.perform(get("/api/creatures/" + CREATURE).header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Owlbear King"))
                .andExpect(jsonPath("$.document.key").value(DOCUMENT))
                .andExpect(jsonPath("$.document.name").value("DM Homebrew"))
                .andExpect(jsonPath("$.derivedFrom").value(DEFAULT_CREATURE));

        List<String> keys = listKeys(DM);
        assertTrue(keys.contains(CREATURE));
        assertEquals(defaultCreatureCount() + 1, keys.size(), "defaults plus their own creature");
    }

    @Test
    void memberSeesSharedCreature() throws Exception {
        mvc.perform(get("/api/creatures/" + CREATURE).header(DevCurrentUser.HEADER, PLAYER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document.key").value(DOCUMENT));

        assertTrue(listKeys(PLAYER).contains(CREATURE));
    }

    @Test
    void strangerCannotSeeIt() throws Exception {
        mvc.perform(get("/api/creatures/" + CREATURE).header(DevCurrentUser.HEADER, STRANGER))
                .andExpect(status().isNotFound());

        List<String> keys = listKeys(STRANGER);
        assertFalse(keys.contains(CREATURE));
        assertEquals(defaultCreatureCount(), keys.size());
    }

    @Test
    void defaultUserCannotSeeIt() throws Exception {
        mvc.perform(get("/api/creatures/" + CREATURE))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyoneSeesDefaultContent() throws Exception {
        for (String user : List.of(DM, PLAYER, STRANGER)) {
            mvc.perform(get("/api/creatures/" + DEFAULT_CREATURE).header(DevCurrentUser.HEADER, user))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.document.key").value("a5e-mm"));
        }
    }

    @Test
    void meReturnsTheHeaderUser() throws Exception {
        mvc.perform(get("/api/me").header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(DM));
        mvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(DevCurrentUser.DEFAULT_USERNAME));
    }

    @Test
    void rejectsInvalidUsernames() throws Exception {
        mvc.perform(get("/api/me").header(DevCurrentUser.HEADER, "not a username!"))
                .andExpect(status().isBadRequest());
    }

    private List<String> listKeys(String user) throws Exception {
        String body = mvc.perform(get("/api/creatures").header(DevCurrentUser.HEADER, user))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[*].key");
    }

    private int defaultCreatureCount() {
        return jdbc.queryForObject("""
                select count(*) from open5e.creatures c join open5e.documents d on d.key = c.document_key
                where d.owner_id is null""", Integer.class);
    }
}
