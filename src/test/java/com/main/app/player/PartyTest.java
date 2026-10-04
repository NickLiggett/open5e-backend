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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Parties through real requests as different {@code dev} users: a DM, their players, and a stranger. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PartyTest {

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
        annasCharacter = createCharacter(ANNA, "Anna's Wizard", null);
        bensCharacter = createCharacter(BEN, "Ben's Rogue", null);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void aPendingInvitationShowsTheDmNothing() throws Exception {
        mvc.perform(put("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ANNA))
                .andExpect(jsonPath("$.status").value("PENDING"));

        mvc.perform(get("/api/party").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
        mvc.perform(get("/api/party/players").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());
        mvc.perform(get("/api/party/invitations").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].dm").value(DM))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void afterAcceptingTheDmSeesTheirCharactersAndOnlyTheirs() throws Exception {
        mvc.perform(put("/api/party/members/@" + ANNA.toUpperCase()).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());

        mvc.perform(post("/api/party/invitations/" + DM + "/accept").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(get("/api/party").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$[0].status").value("ACCEPTED"));
        mvc.perform(get("/api/party/players").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Anna's Wizard"))
                .andExpect(jsonPath("$[0].owner").value(ANNA))
                .andExpect(jsonPath("$[0].role").value("PARTY"));
        mvc.perform(get("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
        mvc.perform(get("/api/players/" + bensCharacter).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0)); // not "theirs"
    }

    @Test
    void theDmSeesCharactersAMemberPlaysToo() throws Exception {
        long borrowed = createCharacter(STRANGER, "Borrowed Bard", BEN);
        join(BEN);

        mvc.perform(get("/api/party/players").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Ben's Rogue"))
                .andExpect(jsonPath("$[1].name").value("Borrowed Bard"));
        mvc.perform(get("/api/players/" + borrowed).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
    }

    @Test
    void theDmCannotChangeOrDeleteAPartyCharacter() throws Exception {
        join(ANNA);

        mvc.perform(put("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, DM)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Mine now\", \"ruleset\": \"5e-2014\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, DM)).andExpect(status().isForbidden());

        mvc.perform(get("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, ANNA))
                .andExpect(jsonPath("$.name").value("Anna's Wizard"));
    }

    @Test
    void charactersTheDmAlreadyOwnsOrPlaysAreNotRepeatedInTheParty() throws Exception {
        long theirs = createCharacter(DM, "DM's NPC", ANNA);
        createCharacter(ANNA, "Anna plays for DM", null);
        jdbc.update("update open5e.player_characters set played_by = (select id from open5e.users where username = ?) where name = ?",
                DM, "Anna plays for DM");
        join(ANNA);

        mvc.perform(get("/api/party/players").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Anna's Wizard"));
        mvc.perform(get("/api/players/" + theirs).header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void leavingEndsTheDmsView_asDoesBeingRemoved() throws Exception {
        join(ANNA);
        join(BEN);

        mvc.perform(delete("/api/party/invitations/" + DM).header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/party/members/" + BEN).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());

        mvc.perform(get("/api/party").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/party/players").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());
        mvc.perform(get("/api/players/" + annasCharacter).header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isOk()); // still hers
        mvc.perform(delete("/api/party/members/" + BEN).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());
    }

    @Test
    void theDmCanWithdrawARequest_andAPlayerCanTurnItDown() throws Exception {
        mvc.perform(put("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
        mvc.perform(put("/api/party/members/" + BEN).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());

        mvc.perform(delete("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/party/invitations/" + DM).header(DevCurrentUser.HEADER, BEN)).andExpect(status().isNoContent());

        mvc.perform(get("/api/party/invitations").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/party").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void askingAgainChangesNothing_andOnlyAskedUsersCanAccept() throws Exception {
        join(ANNA);

        mvc.perform(put("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.status").value("ACCEPTED")); // not sent back to pending

        mvc.perform(post("/api/party/invitations/" + DM + "/accept").header(DevCurrentUser.HEADER, STRANGER))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/party/invitations/" + STRANGER + "/accept").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/party/invitations/" + DM).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
    }

    @Test
    void refusesUnknownUsersAndYourself() throws Exception {
        mvc.perform(put("/api/party/members/nobody-here").header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No user 'nobody-here'"));
        mvc.perform(put("/api/party/members/" + DM).header(DevCurrentUser.HEADER, DM)).andExpect(status().isBadRequest());
        mvc.perform(post("/api/party/invitations/nobody-here/accept").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNotFound());
    }

    @Test
    void limitsThePartySize() throws Exception {
        long dm = userService.findOrCreate(DM).id();
        for (int i = 0; i < PartyService.MAX_MEMBERS; i++) {
            long user = userService.findOrCreate(TestUsers.PREFIX + "member-" + i).id();
            jdbc.update("insert into open5e.party_members (dm_id, user_id) values (?, ?)", dm, user);
        }

        mvc.perform(put("/api/party/members/" + ANNA).header(DevCurrentUser.HEADER, DM)).andExpect(status().isBadRequest());
        mvc.perform(put("/api/party/members/" + TestUsers.PREFIX + "member-0").header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk()); // someone already in it is fine
    }

    @Test
    void deletingTheDmDissolvesTheParty() {
        join(ANNA);

        jdbc.update("delete from open5e.users where username = ?", DM);

        org.assertj.core.api.Assertions.assertThat(
                jdbc.queryForObject("select count(*) from open5e.party_members where user_id = (select id from open5e.users where username = ?)",
                        Integer.class, ANNA)).isZero();
    }

    private void join(String user) {
        jdbc.update("""
                insert into open5e.party_members (dm_id, user_id, status)
                select d.id, u.id, 'ACCEPTED' from open5e.users d, open5e.users u where d.username = ? and u.username = ?
                on conflict (dm_id, user_id) do update set status = 'ACCEPTED'""", DM, user);
    }

    private long createCharacter(String owner, String name, String playedBy) throws Exception {
        String body = "{\"name\": \"%s\", \"ruleset\": \"5e-2014\", \"playedBy\": %s}"
                .formatted(name, playedBy == null ? "null" : "\"" + playedBy + "\"");
        ResultActions result = mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, owner)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
    }
}
