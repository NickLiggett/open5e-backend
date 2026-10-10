package com.main.app.player;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** What the people in a DM's party see of the DM's initiative tracker, as different {@code dev} users. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PartyTrackerTest {

    static final String DM = TestUsers.PREFIX + "dm";
    static final String ANNA = TestUsers.PREFIX + "anna";
    static final String BEN = TestUsers.PREFIX + "ben";
    static final String STRANGER = TestUsers.PREFIX + "stranger";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    private long annasCharacter;
    private long bensCharacter;

    @BeforeEach
    void setUp() throws Exception {
        TestUsers.cleanUp(jdbc);
        for (String user : new String[]{DM, ANNA, BEN, STRANGER}) {
            userService.findOrCreate(user);
        }
        annasCharacter = createCharacter(ANNA, "Anna's Wizard");
        bensCharacter = createCharacter(BEN, "Ben's Rogue");
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    /** A tracker with Anna's wizard, a goblin and a hidden ogre, the DM's notes and the goblin's stat block in it. */
    private String fight(boolean goblinRevealed) {
        return """
                {"version": 1, "combatants": [
                  {"id": 1, "name": "Anna's Wizard", "initiative": 18, "ac": 12, "hp": 30, "type": "PC", "playerId": %d, "reaction": false},
                  {"id": 2, "name": "Goblin", "initiative": 15, "ac": 15, "hp": 7, "type": "Creature", "creatureKey": "srd_goblin",
                   "revealed": %s, "notes": "secret plan"},
                  {"id": 3, "name": "Ogre", "initiative": 9, "ac": 11, "hp": 59, "type": "Creature", "creatureKey": "srd_ogre"}
                ]}""".formatted(annasCharacter, goblinRevealed);
    }

    private void dmSaves(String tracker) throws Exception {
        mvc.perform(put("/api/me/tracker").header(DevCurrentUser.HEADER, DM)
                .contentType(MediaType.APPLICATION_JSON).content(tracker)).andExpect(status().isOk());
    }

    @Test
    void aMemberWhoseCharacterIsInTheFightSeesItWithMonstersStatsHidden() throws Exception {
        join(ANNA);
        dmSaves(fight(false));

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dm").value(DM))
                .andExpect(jsonPath("$[0].combatants.length()").value(3))
                .andExpect(jsonPath("$[0].combatants[0].name").value("Anna's Wizard"))
                .andExpect(jsonPath("$[0].combatants[0].mine").value(true))
                .andExpect(jsonPath("$[0].combatants[0].ac").value(12))
                .andExpect(jsonPath("$[0].combatants[0].hp").value(30))
                .andExpect(jsonPath("$[0].combatants[1].name").value("Goblin"))
                .andExpect(jsonPath("$[0].combatants[1].initiative").value(15))
                .andExpect(jsonPath("$[0].combatants[1].mine").value(false))
                .andExpect(jsonPath("$[0].combatants[1].ac").doesNotExist())
                .andExpect(jsonPath("$[0].combatants[1].hp").doesNotExist())
                .andExpect(jsonPath("$[0].combatants[2].ac").doesNotExist())
                .andExpect(jsonPath("$[0].combatants[2].hp").doesNotExist());
    }

    @Test
    void aMonsterShowsItsStatsOnceTheDmRevealsIt() throws Exception {
        join(ANNA);
        dmSaves(fight(true));

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(jsonPath("$[0].combatants[1].revealed").value(true))
                .andExpect(jsonPath("$[0].combatants[1].ac").value(15))
                .andExpect(jsonPath("$[0].combatants[1].hp").value(7))
                .andExpect(jsonPath("$[0].combatants[2].ac").doesNotExist());
    }

    @Test
    void nothingTheDmKeptBeyondWhatAPlayerMaySeeIsGivenOut() throws Exception {
        join(ANNA);
        dmSaves(fight(true));

        String body = mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("creatureKey").doesNotContain("srd_goblin").doesNotContain("secret plan")
                .doesNotContain("notes").doesNotContain("reaction");
    }

    @Test
    void aMemberWhoseCharacterIsNotInTheFightSeesNothing() throws Exception {
        join(ANNA);
        join(BEN);
        dmSaves(fight(false));

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, BEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aMemberWhoPlaysAnothersCharacterSeesTheFightItIsIn() throws Exception {
        join(BEN);
        jdbc.update("update open5e.player_characters set played_by = (select id from open5e.users where username = ?) where id = ?",
                BEN, annasCharacter);
        dmSaves(fight(false));

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, BEN))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].combatants[0].mine").value(true));
    }

    @Test
    void aPendingInvitationOrAStrangerSeesNothing() throws Exception {
        mvc.perform(put("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
        dmSaves(fight(false));

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, STRANGER)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void leavingTheParty_endsTheView() throws Exception {
        join(ANNA);
        dmSaves(fight(false));
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(1));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/party/invitations/" + DM)
                .header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNoContent());

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void aDmWithoutASavedTrackerOrWithAnUnexpectedOneIsNoProblem() throws Exception {
        join(ANNA);
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));

        dmSaves("{\"combatants\": \"not a list\"}");
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));

        dmSaves("{\"combatants\": [{\"name\": 5, \"playerId\": \"x\", \"ac\": \"12\", \"initiative\": null}]}");
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void itAnswers304WhileNothingHasChanged_andNewDataWhenTheDmReveals() throws Exception {
        join(ANNA);
        dmSaves(fight(false));
        String eTag = mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA).header(HttpHeaders.IF_NONE_MATCH, eTag))
                .andExpect(status().isNotModified());

        dmSaves(fight(true));
        mvc.perform(get("/api/party/trackers").header(DevCurrentUser.HEADER, ANNA).header(HttpHeaders.IF_NONE_MATCH, eTag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].combatants[1].hp").value(7));
    }

    @Test
    void theDmsOwnTrackerIsStillTheirOwn() throws Exception {
        join(ANNA);
        dmSaves(fight(false));

        mvc.perform(get("/api/me/tracker").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.combatants").doesNotExist());
        mvc.perform(get("/api/me/tracker").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.combatants[1].creatureKey").value("srd_goblin"));
    }

    private void join(String user) throws Exception {
        mvc.perform(put("/api/party/members/" + user).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
        mvc.perform(post("/api/party/invitations/" + DM + "/accept").header(DevCurrentUser.HEADER, user)).andExpect(status().isOk());
    }

    private long createCharacter(String owner, String name) throws Exception {
        String body = "{\"name\": \"%s\", \"ruleset\": \"5e-2014\"}".formatted(name);
        ResultActions result = mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, owner)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
