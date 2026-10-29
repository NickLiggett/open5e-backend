package com.main.app.profile;

import com.jayway.jsonpath.JsonPath;
import com.main.app.common.TestUsers;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A DM's trackers put aside and swapped back, through real requests as different {@code dev} users. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SavedTrackersTest {

    static final String DM = TestUsers.PREFIX + "dm";
    static final String OTHER = TestUsers.PREFIX + "other";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        TestUsers.cleanUp(jdbc);
        userService.findOrCreate(DM);
        userService.findOrCreate(OTHER);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    private ResultActions as(MockHttpServletRequestBuilder request, String user) throws Exception {
        return mvc.perform(request.header(DevCurrentUser.HEADER, user));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String user, String body) throws Exception {
        return as(request.contentType(MediaType.APPLICATION_JSON).content(body), user);
    }

    private void saveActive(String user, String name, int combatants) throws Exception {
        StringBuilder rows = new StringBuilder();
        for (int i = 1; i <= combatants; i++) {
            rows.append(i > 1 ? "," : "").append("{\"id\": ").append(i).append(", \"name\": \"C").append(i).append("\"}");
        }
        String named = name == null ? "" : "\"name\": \"" + name + "\", ";
        json(put("/api/me/tracker"), user, "{" + named + "\"version\": 1, \"combatants\": [" + rows + "]}").andExpect(status().isOk());
    }

    private long firstSavedId(String user) throws Exception {
        String body = as(get("/api/me/trackers"), user).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    @Test
    void startsWithNothingPutAside() throws Exception {
        as(get("/api/me/trackers"), DM).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aNewTrackerPutsTheOneInUseAsideAndStartsEmpty() throws Exception {
        saveActive(DM, "Tuesday group", 2);

        json(post("/api/me/trackers"), DM, "{\"name\": \"  Friday group \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Friday group"))
                .andExpect(jsonPath("$.combatants.length()").value(0));

        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$.name").value("Friday group"));
        as(get("/api/me/trackers"), DM)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Tuesday group"))
                .andExpect(jsonPath("$[0].combatants").value(2))
                .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test
    void anEmptyUnnamedTrackerIsNotPutAside_andAnUnnamedOneWithPeopleInItIsCalledUntitled() throws Exception {
        json(post("/api/me/trackers"), DM, "{\"name\": \"First\"}").andExpect(status().isOk());
        as(get("/api/me/trackers"), DM).andExpect(jsonPath("$.length()").value(0));

        saveActive(DM, null, 1);
        json(post("/api/me/trackers"), DM, "{\"name\": \"Second\"}").andExpect(status().isOk());
        as(get("/api/me/trackers"), DM)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Untitled tracker"));
    }

    @Test
    void refusesANewTrackerWithoutAUsableName() throws Exception {
        json(post("/api/me/trackers"), DM, "{}").andExpect(status().isBadRequest());
        json(post("/api/me/trackers"), DM, "{\"name\": \"   \"}").andExpect(status().isBadRequest());
        json(post("/api/me/trackers"), DM, "{\"name\": \"" + "x".repeat(61) + "\"}").andExpect(status().isBadRequest());
    }

    @Test
    void openingOneSwapsItWithTheOneInUse_andBackAgain() throws Exception {
        saveActive(DM, "Tuesday group", 2);
        json(post("/api/me/trackers"), DM, "{\"name\": \"Friday group\"}").andExpect(status().isOk());
        saveActive(DM, "Friday group", 3);
        long tuesday = firstSavedId(DM);

        as(post("/api/me/trackers/" + tuesday + "/open"), DM)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tuesday group"))
                .andExpect(jsonPath("$.combatants.length()").value(2));
        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$.name").value("Tuesday group"));
        as(get("/api/me/trackers"), DM)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Friday group"))
                .andExpect(jsonPath("$[0].combatants").value(3));

        as(post("/api/me/trackers/" + firstSavedId(DM) + "/open"), DM)
                .andExpect(jsonPath("$.name").value("Friday group"))
                .andExpect(jsonPath("$.combatants.length()").value(3));
        as(get("/api/me/trackers"), DM)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Tuesday group"));
    }

    @Test
    void openingOneWithoutANameInItsStateGivesItTheNameItWasPutAsideUnder() throws Exception {
        jdbc.update("insert into open5e.user_saved_trackers (user_id, name, state) values "
                + "((select id from open5e.users where username = ?), 'Old one', '{\"combatants\": []}'::jsonb)", DM);

        as(post("/api/me/trackers/" + firstSavedId(DM) + "/open"), DM).andExpect(jsonPath("$.name").value("Old one"));
    }

    @Test
    void anotherUsersTrackerCannotBeOpenedOrDeleted_andMissingOnesAre404() throws Exception {
        saveActive(DM, "Mine", 1);
        json(post("/api/me/trackers"), DM, "{\"name\": \"Next\"}").andExpect(status().isOk());
        long mine = firstSavedId(DM);

        as(post("/api/me/trackers/" + mine + "/open"), OTHER).andExpect(status().isNotFound());
        as(delete("/api/me/trackers/" + mine), OTHER).andExpect(status().isNotFound());
        as(post("/api/me/trackers/0/open"), DM).andExpect(status().isNotFound());
        as(get("/api/me/trackers"), OTHER).andExpect(jsonPath("$.length()").value(0));
        as(get("/api/me/trackers"), DM).andExpect(jsonPath("$.length()").value(1));
        as(get("/api/me/tracker"), OTHER).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void deletesOnePutAside() throws Exception {
        saveActive(DM, "Mine", 1);
        json(post("/api/me/trackers"), DM, "{\"name\": \"Next\"}").andExpect(status().isOk());

        as(delete("/api/me/trackers/" + firstSavedId(DM)), DM).andExpect(status().isNoContent());

        as(get("/api/me/trackers"), DM).andExpect(jsonPath("$.length()").value(0));
        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$.name").value("Next"));
    }

    @Test
    void refusesToPutAsideMoreThanTheMost_butOpeningOneIsStillPossible() throws Exception {
        long user = jdbc.queryForObject("select id from open5e.users where username = ?", Long.class, DM);
        for (int i = 0; i < ProfileService.MAX_SAVED_TRACKERS; i++) {
            jdbc.update("insert into open5e.user_saved_trackers (user_id, name, state) values (?, ?, '{\"combatants\": []}'::jsonb)",
                    user, "Tracker " + i);
        }
        saveActive(DM, "In use", 1);

        json(post("/api/me/trackers"), DM, "{\"name\": \"One too many\"}").andExpect(status().isConflict());
        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$.name").value("In use"));

        // swapping frees the place it takes
        as(post("/api/me/trackers/" + firstSavedId(DM) + "/open"), DM).andExpect(status().isOk());
        as(get("/api/me/trackers"), DM).andExpect(jsonPath("$.length()").value(ProfileService.MAX_SAVED_TRACKERS));
    }

    @Test
    void trackersPutAsideAreDeletedWithTheUser() {
        jdbc.update("insert into open5e.user_saved_trackers (user_id, name, state) values "
                + "((select id from open5e.users where username = ?), 'x', '{}'::jsonb)", DM);

        TestUsers.cleanUp(jdbc);

        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select count(*) from open5e.user_saved_trackers", Integer.class)).isZero();
    }
}
