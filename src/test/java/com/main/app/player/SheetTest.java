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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rest of a character sheet, kept as JSON the app shapes: who can replace it, and what the backend insists on. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SheetTest {

    static final String OWNER = TestUsers.PREFIX + "sheetowner";
    static final String PLAYER = TestUsers.PREFIX + "sheetplayer";
    static final String DM = TestUsers.PREFIX + "sheetdm";
    static final String STRANGER = TestUsers.PREFIX + "sheetstranger";

    static final String CHARACTER = """
            {"name": "Dante", "ruleset": "5e-2014", "hitPoints": 52, "playedBy": "%s"}""".formatted(PLAYER);
    static final String SHEET = """
            {"abilities": {"str": 12, "dex": 20, "con": 14, "int": 12, "wis": 16, "cha": 10},
             "skills": {"stealth": "expertise", "arcana": "proficient"},
             "speed": {"walk": 35}, "senses": ["Darkvision 120 ft."],
             "attacks": [{"name": "Longsword", "bonus": 9, "damage": "1d8+5 slashing"}],
             "personal": {"alignment": "Chaotic Good", "backstory": "Long ago..."}}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        TestUsers.cleanUp(jdbc);
        userService.findOrCreate(OWNER);
        userService.findOrCreate(PLAYER);
        userService.findOrCreate(DM);
        userService.findOrCreate(STRANGER);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void aNewCharacterHasAnEmptySheet() throws Exception {
        long id = create();

        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheet").isMap())
                .andExpect(jsonPath("$.sheet").isEmpty());
    }

    @Test
    void theOwnerReplacesTheSheetAndGetsItBackAsItWas() throws Exception {
        long id = create();

        replace(OWNER, id, "{\"sheet\": " + SHEET + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheet.abilities.dex").value(20))
                .andExpect(jsonPath("$.sheet.skills.stealth").value("expertise"))
                .andExpect(jsonPath("$.sheet.senses[0]").value("Darkvision 120 ft."))
                .andExpect(jsonPath("$.sheet.attacks[0].damage").value("1d8+5 slashing"))
                .andExpect(jsonPath("$.sheet.personal.backstory").value("Long ago..."))
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, PLAYER))
                .andExpect(jsonPath("$.sheet.speed.walk").value(35));

        replace(OWNER, id, "{\"sheet\": {\"speed\": {\"walk\": 30}}}") // replaced, not merged
                .andExpect(jsonPath("$.sheet.speed.walk").value(30))
                .andExpect(jsonPath("$.sheet.abilities").doesNotExist());
    }

    @Test
    void thePlayerCanFillItInButADmCannotAndAStrangerCannotSeeIt() throws Exception {
        long id = create();
        long dm = userService.findOrCreate(DM).id();
        jdbc.update("insert into open5e.party_members (dm_id, user_id, status) values (?, ?, 'ACCEPTED')", dm,
                userService.findOrCreate(OWNER).id());

        replace(PLAYER, id, "{\"sheet\": {\"speed\": {\"walk\": 40}}}").andExpect(status().isOk());
        replace(DM, id, "{\"sheet\": {\"speed\": {\"walk\": 10}}}").andExpect(status().isForbidden());
        replace(STRANGER, id, "{\"sheet\": {}}").andExpect(status().isNotFound());
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, DM)) // the DM can read it
                .andExpect(jsonPath("$.sheet.speed.walk").value(40));
    }

    @Test
    void savingTheCharacterDoesNotTouchTheSheet_andSavingTheSheetDoesNotTouchWhatChangesInPlay() throws Exception {
        long id = create();
        replace(OWNER, id, "{\"sheet\": " + SHEET + "}").andExpect(status().isOk());
        mvc.perform(patch("/api/players/" + id + "/vitals").header(DevCurrentUser.HEADER, OWNER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentHp\": 20, \"conditions\": [\"Prone\"]}"));

        mvc.perform(put("/api/players/" + id).header(DevCurrentUser.HEADER, OWNER).contentType(MediaType.APPLICATION_JSON)
                        .content(CHARACTER.replace("Dante", "Dante Von Ashcroft")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sheet.abilities.dex").value(20));
        replace(OWNER, id, "{\"sheet\": {\"speed\": {\"walk\": 35}}}")
                .andExpect(jsonPath("$.vitals.currentHp").value(20))
                .andExpect(jsonPath("$.vitals.conditions[0]").value("Prone"));
    }

    @Test
    void aSheetFromAStaleCopyIsRefused() throws Exception {
        long id = create();

        replace(OWNER, id, "{\"version\": 0, \"sheet\": {\"speed\": {\"walk\": 30}}}").andExpect(status().isOk());
        replace(OWNER, id, "{\"version\": 0, \"sheet\": {\"speed\": {\"walk\": 99}}}").andExpect(status().isConflict());
        replace(OWNER, id, "{\"version\": 1, \"sheet\": {\"speed\": {\"walk\": 35}}}").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, OWNER))
                .andExpect(jsonPath("$.sheet.speed.walk").value(35));
    }

    @Test
    void refusesWhatIsNotAnObjectOrIsTooBigDeepOrLong() throws Exception {
        long id = create();
        String deep = "{\"a\":".repeat(SheetChecks.MAX_DEPTH + 1) + "1" + "}".repeat(SheetChecks.MAX_DEPTH + 1);
        String longText = "x".repeat(SheetChecks.MAX_TEXT + 1);
        String wide = "[" + "1,".repeat(SheetChecks.MAX_ENTRIES) + "1]";
        String big = "{\"lines\": [" + ("\"" + "y".repeat(SheetChecks.MAX_TEXT - 1) + "\",").repeat(11) + "\"z\"]}";

        for (String body : new String[]{"{}", "{\"sheet\": []}", "{\"sheet\": \"text\"}", "{\"sheet\": null}", "{\"sheet\": 5}",
                "{\"sheet\": " + deep + "}", "{\"sheet\": {\"backstory\": \"" + longText + "\"}}", "{\"sheet\": {\"list\": " + wide + "}}",
                "{\"sheet\": " + big + "}"}) {
            replace(OWNER, id, body).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/players/" + id).header(DevCurrentUser.HEADER, OWNER)).andExpect(jsonPath("$.sheet").isEmpty());
    }

    @Test
    void anUnknownCharacterIsNotFound() throws Exception {
        replace(OWNER, 999999999L, "{\"sheet\": {}}").andExpect(status().isNotFound());
    }

    private ResultActions replace(String user, long id, String body) throws Exception {
        return mvc.perform(put("/api/players/" + id + "/sheet").header(DevCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long create() throws Exception {
        String response = mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON).content(CHARACTER))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
