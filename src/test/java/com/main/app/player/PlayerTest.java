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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Player characters through real requests as different {@code dev} users: a DM who owns them, and who plays them. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PlayerTest {

    static final String DM = TestUsers.PREFIX + "dm";
    static final String PLAYER = TestUsers.PREFIX + "player";
    static final String STRANGER = TestUsers.PREFIX + "stranger";

    static final String THORIN = """
            {"name": "  Thorin Oakenshield ", "ruleset": "5e-2014", "classKey": "srd_fighter", "className": "Fighter",
             "speciesName": "Dwarf", "level": 5, "armorClass": 18, "hitPoints": 52, "initiativeBonus": 1,
             "notes": "Carries a grudge", "playedBy": "%s"}""".formatted(PLAYER);

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
        userService.findOrCreate(PLAYER);
        userService.findOrCreate(STRANGER);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void ownerMakesAndReadsACharacter() throws Exception {
        long id = create(DM, THORIN);

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thorin Oakenshield"))
                .andExpect(jsonPath("$.ruleset").value("5e-2014"))
                .andExpect(jsonPath("$.className").value("Fighter"))
                .andExpect(jsonPath("$.classKey").value("srd_fighter"))
                .andExpect(jsonPath("$.speciesName").value("Dwarf"))
                .andExpect(jsonPath("$.speciesKey").doesNotExist())
                .andExpect(jsonPath("$.level").value(5))
                .andExpect(jsonPath("$.armorClass").value(18))
                .andExpect(jsonPath("$.hitPoints").value(52))
                .andExpect(jsonPath("$.initiativeBonus").value(1))
                .andExpect(jsonPath("$.owner").value(DM))
                .andExpect(jsonPath("$.playedBy").value(PLAYER))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void onlyNameAndRulesAreNeeded() throws Exception {
        long id = create(DM, "{\"name\": \"Mouse\", \"ruleset\": \"5e-2024\"}");

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.initiativeBonus").value(0))
                .andExpect(jsonPath("$.armorClass").doesNotExist())
                .andExpect(jsonPath("$.hitPoints").doesNotExist())
                .andExpect(jsonPath("$.playedBy").doesNotExist());
    }

    @Test
    void listsWhatTheUserOwnsOrPlaysByName() throws Exception {
        create(DM, THORIN);
        create(DM, "{\"name\": \"Aria\", \"ruleset\": \"5e-2024\"}");
        create(PLAYER, "{\"name\": \"Zed\", \"ruleset\": \"5e-2014\"}");

        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Aria"))
                .andExpect(jsonPath("$[1].name").value("Thorin Oakenshield"));
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, PLAYER))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Thorin Oakenshield"))
                .andExpect(jsonPath("$[0].role").value("PLAYER"))
                .andExpect(jsonPath("$[1].name").value("Zed"))
                .andExpect(jsonPath("$[1].role").value("OWNER"));
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, STRANGER))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void theOwnerReplacesAnyPart() throws Exception {
        long id = create(DM, THORIN);

        mvc.perform(put("/api/players/" + id).header(DevCurrentUser.HEADER, DM).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Thorin II\", \"ruleset\": \"5e-2024\", \"level\": 6, \"playedBy\": \"@" + STRANGER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Thorin II"))
                .andExpect(jsonPath("$.ruleset").value("5e-2024"))
                .andExpect(jsonPath("$.level").value(6))
                .andExpect(jsonPath("$.className").doesNotExist()) // replaced, not merged
                .andExpect(jsonPath("$.playedBy").value(STRANGER));

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isOk());
    }

    @Test
    void thePlayerKeepsTheNumbersUpToDate() throws Exception {
        long id = create(DM, THORIN);

        mvc.perform(put("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER).contentType(MediaType.APPLICATION_JSON)
                        .content(THORIN.replace("\"level\": 5", "\"level\": 6").replace("52", "60").replace("Carries a grudge", "Found a sword")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(6))
                .andExpect(jsonPath("$.hitPoints").value(60))
                .andExpect(jsonPath("$.notes").value("Found a sword"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void thePlayerCannotChangeTheOwnersParts() throws Exception {
        long id = create(DM, THORIN);

        for (String changed : new String[]{
                THORIN.replace("Thorin Oakenshield", "Thrain"),
                THORIN.replace("5e-2014", "5e-2024"),
                THORIN.replace("Fighter", "Rogue"),
                THORIN.replace("Dwarf", "Elf"),
                THORIN.replace("\"playedBy\": \"" + PLAYER + "\"", "\"playedBy\": null")}) {
            mvc.perform(put("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER)
                            .contentType(MediaType.APPLICATION_JSON).content(changed))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.name").value("Thorin Oakenshield"))
                .andExpect(jsonPath("$.className").value("Fighter"));
    }

    @Test
    void thePlayerCannotDeleteIt_andStrangersCannotSeeOrChangeIt() throws Exception {
        long id = create(DM, THORIN);

        mvc.perform(delete("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER)).andExpect(status().isForbidden());
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
        mvc.perform(put("/api/players/" + id).header(DevCurrentUser.HEADER, STRANGER).contentType(MediaType.APPLICATION_JSON)
                .content(THORIN)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/players/" + id).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
    }

    @Test
    void theOwnerDeletesIt() throws Exception {
        long id = create(DM, THORIN);

        mvc.perform(delete("/api/players/" + id).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER)).andExpect(status().isNotFound());
    }

    @Test
    void playingYourOwnCharacterIsNoSpecialMention() throws Exception {
        long id = create(DM, "{\"name\": \"Solo\", \"ruleset\": \"5e-2014\", \"playedBy\": \"" + DM + "\"}");

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.playedBy").doesNotExist());
    }

    @Test
    void anUnknownPlayerIsNotFound() throws Exception {
        send(DM, "{\"name\": \"Ghosted\", \"ruleset\": \"5e-2014\", \"playedBy\": \"nobody-here\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No user 'nobody-here'"));
    }

    @Test
    void deletingTheUserWhoPlaysItLeavesTheCharacterUnassigned() throws Exception {
        long id = create(DM, THORIN);

        jdbc.update("delete from open5e.users where username = ?", PLAYER);

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playedBy").doesNotExist());
    }

    @Test
    void refusesNonsense() throws Exception {
        String ok = "\"name\": \"X\", \"ruleset\": \"5e-2014\"";
        for (String body : new String[]{
                "{}",
                "{\"name\": \"  \", \"ruleset\": \"5e-2014\"}",
                "{\"name\": \"X\"}",
                "{\"name\": \"X\", \"ruleset\": \"a5e\"}",
                "{\"name\": \"" + "n".repeat(61) + "\", \"ruleset\": \"5e-2014\"}",
                "{" + ok + ", \"level\": 0}",
                "{" + ok + ", \"level\": 21}",
                "{" + ok + ", \"armorClass\": -1}",
                "{" + ok + ", \"armorClass\": 41}",
                "{" + ok + ", \"hitPoints\": 10000}",
                "{" + ok + ", \"initiativeBonus\": 31}",
                "{" + ok + ", \"notes\": \"" + "n".repeat(5001) + "\"}"}) {
            send(DM, body).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void limitsHowManyOneUserOwns() throws Exception {
        long dm = userService.findOrCreate(DM).id();
        for (int i = 0; i < PlayerService.MAX_OWNED; i++) {
            jdbc.update("insert into open5e.player_characters (owner_id, name, ruleset) values (?, ?, '5e-2014')", dm, "P" + i);
        }

        send(DM, "{\"name\": \"One too many\", \"ruleset\": \"5e-2014\"}").andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from open5e.player_characters where owner_id = ?", Integer.class, dm))
                .isEqualTo(PlayerService.MAX_OWNED);
    }

    private ResultActions send(String user, String body) throws Exception {
        return mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long create(String user, String body) throws Exception {
        String response = send(user, body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
