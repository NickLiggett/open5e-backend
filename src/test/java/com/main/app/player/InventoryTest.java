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

/** What a character carries, through real requests as an owner, the user who plays it, and strangers. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InventoryTest {

    static final String DM = TestUsers.PREFIX + "invdm";
    static final String PLAYER = TestUsers.PREFIX + "invplayer";
    static final String STRANGER = TestUsers.PREFIX + "invstranger";

    static final String BAG = """
            {"gold": 15, "silver": 3, "copper": 7, "items": [
              {"itemKey": "srd_rope", "kind": "item", "name": " Rope ", "quantity": 2, "notes": "50 ft"},
              {"itemKey": "srd_cloak", "kind": "MAGIC", "name": "Cloak of Protection", "equipped": true, "attuned": true},
              {"name": "Grandfather's locket", "kind": "ITEM"}]}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    private long character;

    @BeforeEach
    void setUp() throws Exception {
        TestUsers.cleanUp(jdbc);
        userService.findOrCreate(DM);
        userService.findOrCreate(PLAYER);
        userService.findOrCreate(STRANGER);
        String body = "{\"name\": \"Thorin\", \"ruleset\": \"5e-2014\", \"playedBy\": \"" + PLAYER + "\"}";
        String response = mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, DM)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        character = ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void startsEmpty() throws Exception {
        mvc.perform(get(url()).header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(0))
                .andExpect(jsonPath("$.silver").value(0))
                .andExpect(jsonPath("$.copper").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void theOwnerKeepsCoinsAndItemsInOrder() throws Exception {
        replace(DM, BAG).andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(15))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].name").value("Rope"))
                .andExpect(jsonPath("$.items[0].kind").value("ITEM"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].notes").value("50 ft"))
                .andExpect(jsonPath("$.items[1].attuned").value(true))
                .andExpect(jsonPath("$.items[1].equipped").value(true))
                .andExpect(jsonPath("$.items[2].name").value("Grandfather's locket"))
                .andExpect(jsonPath("$.items[2].itemKey").doesNotExist())
                .andExpect(jsonPath("$.items[2].kind").doesNotExist()) // a typed line has no kind
                .andExpect(jsonPath("$.items[2].quantity").value(1));

        mvc.perform(get(url()).header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.items[1].name").value("Cloak of Protection"));
    }

    @Test
    void replacingIsReplacingNotMerging() throws Exception {
        replace(DM, BAG).andExpect(status().isOk());

        replace(DM, "{\"items\": [{\"name\": \"Torch\"}]}").andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(0))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void thePlayerCanChangeItToo() throws Exception {
        replace(PLAYER, BAG).andExpect(status().isOk());

        mvc.perform(get(url()).header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.items.length()").value(3));
    }

    @Test
    void aStrangerCannotSeeItAndAPartyDmCanOnlyRead() throws Exception {
        replace(DM, BAG).andExpect(status().isOk());

        mvc.perform(get(url()).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
        replace(STRANGER, BAG).andExpect(status().isNotFound());

        long owner = userService.findOrCreate(DM).id();
        long stranger = userService.findOrCreate(STRANGER).id();
        jdbc.update("insert into open5e.party_members (dm_id, user_id, status) values (?, ?, 'ACCEPTED')", stranger, owner);

        mvc.perform(get(url()).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(15));
        replace(STRANGER, BAG).andExpect(status().isForbidden());
    }

    @Test
    void deletingTheCharacterDeletesWhatItCarried() throws Exception {
        replace(DM, BAG).andExpect(status().isOk());

        mvc.perform(delete("/api/players/" + character).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from open5e.player_character_items where character_id = ?",
                Integer.class, character)).isZero();
    }

    @Test
    void refusesNonsense() throws Exception {
        String four = "{\"name\": \"A\", \"attuned\": true},".repeat(InventoryService.MAX_ATTUNED + 1);
        String many = "{\"name\": \"A\"},".repeat(InventoryService.MAX_LINES) + "{\"name\": \"B\"}";
        for (String body : new String[]{
                "{\"gold\": -1}",
                "{\"copper\": 10000000}",
                "{\"items\": [{\"name\": \"  \"}]}",
                "{\"items\": [{\"name\": \"A\", \"quantity\": 0}]}",
                "{\"items\": [{\"name\": \"A\", \"quantity\": 10000}]}",
                "{\"items\": [{\"name\": \"" + "n".repeat(121) + "\"}]}",
                "{\"items\": [{\"name\": \"A\", \"notes\": \"" + "n".repeat(501) + "\"}]}",
                "{\"items\": [{\"name\": \"A\", \"itemKey\": \"srd_a\", \"kind\": \"WEAPON\"}]}",
                "{\"items\": [{\"name\": \"A\", \"itemKey\": \"srd_a\"}]}",
                "{\"items\": [" + four.substring(0, four.length() - 1) + "]}",
                "{\"items\": [" + many + "]}"}) {
            replace(DM, body).andExpect(status().isBadRequest());
        }
        mvc.perform(get(url()).header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.items.length()").value(0));
    }

    private String url() {
        return "/api/players/" + character + "/inventory";
    }

    private ResultActions replace(String user, String body) throws Exception {
        return mvc.perform(put(url()).header(DevCurrentUser.HEADER, user).contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
