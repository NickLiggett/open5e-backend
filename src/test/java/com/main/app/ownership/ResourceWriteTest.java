package com.main.app.ownership;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creating, changing, deleting and copying resources, and sharing documents, through real requests as different
 * users in the {@code dev} profile.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ResourceWriteTest {

    static final String DM = TestUsers.PREFIX + "dm";
    static final String PLAYER = TestUsers.PREFIX + "player";
    static final String STRANGER = TestUsers.PREFIX + "stranger";
    static final String ABOLETH = "a5e-mm_aboleth";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    private String dmHomebrew;

    @BeforeEach
    void setUp() {
        TestUsers.cleanUp(jdbc);
        dmHomebrew = Keys.userDocument(userService.findOrCreate(DM).id(), "homebrew");
        userService.findOrCreate(PLAYER);
        userService.findOrCreate(STRANGER);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void createsInThePersonalDocumentByDefault() throws Exception {
        String key = dmHomebrew + "_owlbear-king";
        send(post("/api/creatures"), DM, """
                {"name": "Owlbear King", "challengeRating": 5,
                 "type": {"key": "monstrosity", "name": "Monstrosity"},
                 "speed": {"unit": "feet", "walk": 40}}""")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/creatures/" + key))
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.document.key").value(dmHomebrew))
                .andExpect(jsonPath("$.document.type").value("HOMEBREW"))
                .andExpect(jsonPath("$.speed.walk").value(40))
                .andExpect(jsonPath("$.derivedFrom").value(nullValue()));

        as(get("/api/creatures/" + key), DM).andExpect(status().isOk());
        as(get("/api/creatures/" + key), STRANGER).andExpect(status().isNotFound());
        as(get("/api/documents/" + dmHomebrew), DM).andExpect(jsonPath("$.name").value(DM + "'s homebrew"));
    }

    @Test
    void createsInANamedDocumentWithFiltersWorking() throws Exception {
        String document = createDocument(DM, "Curse of the Owlbear");
        assertEquals("u" + userService.findOrCreate(DM).id() + "-curse-of-the-owlbear", document);

        send(post("/api/spells"), DM, """
                {"document": "%s", "name": "Owl Bolt", "level": 1,
                 "school": {"key": "evocation", "name": "Evocation"},
                 "classes": [{"key": "srd-2024_wizard", "name": "Wizard"}], "damageTypes": ["force"]}""".formatted(document))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value(document + "_owl-bolt"));
        as(get("/api/spells?document=%s&class=srd-2024_wizard&damageType=force&level=1&school=evocation".formatted(document)), DM)
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // Key columns used by filters follow the JSON objects they come from
        send(post("/api/magicitems"), DM, """
                {"document": "%s", "name": "Owlbear Wand", "rarity": {"key": "rare", "name": "Rare", "rank": 3},
                 "category": {"key": "wand", "name": "Wand"}}""".formatted(document))
                .andExpect(status().isCreated());
        as(get("/api/magicitems?document=%s&rarity=rare&category=wand".formatted(document)), DM)
                .andExpect(jsonPath("$.page.totalElements").value(1));
        send(patch("/api/magicitems/" + document + "_owlbear-wand"), DM, """
                {"rarity": {"key": "legendary", "name": "Legendary", "rank": 5}}""")
                .andExpect(status().isOk());
        as(get("/api/magicitems?document=%s&rarity=legendary".formatted(document)), DM)
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void replacesAndUpdates() throws Exception {
        String key = createCreature(DM, "Owlbear King");

        send(patch("/api/creatures/" + key), DM, "{\"hitPoints\": 120}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hitPoints").value(120))
                .andExpect(jsonPath("$.name").value("Owlbear King"));

        send(put("/api/creatures/" + key), DM, "{\"name\": \"Owlbear Emperor\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(key))
                .andExpect(jsonPath("$.name").value("Owlbear Emperor"))
                .andExpect(jsonPath("$.hitPoints").value(nullValue()));
    }

    @Test
    void aFetchedResourceCanBeSentBackAsItIs() throws Exception {
        String copy = copy("/api/creatures/" + ABOLETH, DM);
        String fetched = as(get("/api/creatures/" + copy), DM).andReturn().getResponse().getContentAsString();

        String replaced = send(put("/api/creatures/" + copy), DM, fetched)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(fetched, replaced);
    }

    @Test
    void rejectsInvalidBodies() throws Exception {
        String key = createCreature(DM, "Owlbear King");

        send(patch("/api/creatures/" + key), DM, "{\"hitPionts\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown field 'hitPionts' for a creature"));
        send(patch("/api/creatures/" + key), DM, "{\"hitPoints\": \"lots\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("'hitPoints'")));
        send(patch("/api/creatures/" + key), DM, "{\"actions\": [{\"actionType\": \"NAP\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("'actions[0].actionType'")));
        send(put("/api/creatures/" + key), DM, "{\"hitPoints\": 10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A creature needs a name"));
        send(post("/api/creatures"), DM, "{\"name\": \"Owlbear King\"}")
                .andExpect(status().isConflict());
        send(post("/api/creatures"), DM, "{\"name\": \"!!!\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void defaultContentIsReadOnly() throws Exception {
        send(put("/api/creatures/" + ABOLETH), DM, "{\"name\": \"Mine now\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("Copy it")));
        as(delete("/api/creatures/" + ABOLETH), DM).andExpect(status().isForbidden());
        send(post("/api/creatures"), DM, "{\"document\": \"a5e-mm\", \"name\": \"Sneaky\"}")
                .andExpect(status().isForbidden());
        as(get("/api/creatures/" + ABOLETH), STRANGER).andExpect(jsonPath("$.name").value("Aboleth"));
    }

    @Test
    void otherUsersContentIsNotFound() throws Exception {
        String key = createCreature(DM, "Owlbear King");

        send(patch("/api/creatures/" + key), STRANGER, "{\"hitPoints\": 1}").andExpect(status().isNotFound());
        as(delete("/api/creatures/" + key), STRANGER).andExpect(status().isNotFound());
        as(post("/api/creatures/" + key + "/copy"), STRANGER).andExpect(status().isNotFound());
        send(post("/api/creatures"), STRANGER, "{\"document\": \"%s\", \"name\": \"Intruder\"}".formatted(dmHomebrew))
                .andExpect(status().isNotFound());
    }

    @Test
    void copiesDefaultContentToCustomizeIt() throws Exception {
        String copy = copy("/api/creatures/" + ABOLETH, DM);
        assertEquals(dmHomebrew + "_aboleth", copy);

        as(get("/api/creatures/" + copy), DM)
                .andExpect(jsonPath("$.derivedFrom").value(ABOLETH))
                .andExpect(jsonPath("$.name").value("Aboleth"))
                .andExpect(jsonPath("$.actions[0].name").value("Baleful Charm"));

        send(patch("/api/creatures/" + copy), DM, "{\"name\": \"Elder Aboleth\", \"hitPoints\": 300}")
                .andExpect(status().isOk());

        // Shown alongside the original, which is unchanged for everyone
        as(get("/api/creatures?name=aboleth&pageSize=100"), DM)
                .andExpect(jsonPath("$.content[*].key", hasItem(ABOLETH)))
                .andExpect(jsonPath("$.content[*].key", hasItem(copy)));
        as(get("/api/creatures/" + ABOLETH), DM).andExpect(jsonPath("$.name").value("Aboleth"));
        as(get("/api/creatures?name=aboleth&pageSize=100"), STRANGER)
                .andExpect(jsonPath("$.content[*].key", not(hasItem(copy))));

        // A second copy gets the next free key
        assertEquals(dmHomebrew + "_aboleth-2", copy("/api/creatures/" + ABOLETH, DM));
    }

    @Test
    void sharesDocuments() throws Exception {
        String document = createDocument(DM, "Party Loot");
        String sword = createIn(document, DM, "/api/magicitems", "Sword of Owls");

        // Viewer: can see, can't change
        share(document, PLAYER, "VIEWER").andExpect(status().isOk());
        as(get("/api/magicitems/" + sword), PLAYER).andExpect(status().isOk());
        send(patch("/api/magicitems/" + sword), PLAYER, "{\"desc\": \"mine\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(containsString("view")));
        as(get("/api/magicitems/" + sword), STRANGER).andExpect(status().isNotFound());

        // Editor: can change and add, can't manage members or the document
        share(document, PLAYER, "EDITOR").andExpect(status().isOk());
        send(patch("/api/magicitems/" + sword), PLAYER, "{\"desc\": \"Hoots when drawn\"}").andExpect(status().isOk());
        createIn(document, PLAYER, "/api/magicitems", "Owl Feather");
        send(put("/api/documents/%s/members/%s".formatted(document, STRANGER)), PLAYER, "{\"role\": \"VIEWER\"}")
                .andExpect(status().isForbidden());
        as(delete("/api/documents/" + document), PLAYER).andExpect(status().isForbidden());

        as(get("/api/documents/%s/members".formatted(document)), PLAYER)
                .andExpect(jsonPath("$[0].username").value(DM))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].username").value(PLAYER))
                .andExpect(jsonPath("$[1].role").value("EDITOR"));
        as(get("/api/documents/%s/members".formatted(document)), STRANGER).andExpect(status().isNotFound());

        // Members can leave
        as(delete("/api/documents/%s/members/%s".formatted(document, PLAYER)), PLAYER).andExpect(status().isNoContent());
        as(get("/api/magicitems/" + sword), PLAYER).andExpect(status().isNotFound());
    }

    @Test
    void validatesSharing() throws Exception {
        String document = createDocument(DM, "Party Loot");

        share(document, PLAYER, "ADMIN").andExpect(status().isBadRequest());
        share(document, DM, "EDITOR").andExpect(status().isBadRequest());
        share(document, TestUsers.PREFIX + "nobody", "VIEWER").andExpect(status().isNotFound());
        send(put("/api/documents/srd-2024/members/" + PLAYER), DM, "{\"role\": \"VIEWER\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void deletesResourcesAndDocuments() throws Exception {
        String document = createDocument(DM, "Doomed");
        String owlbear = createIn(document, DM, "/api/creatures", "Owlbear");
        String spell = createIn(document, DM, "/api/spells", "Owl Bolt");
        String other = createCreature(DM, "Survivor");

        as(delete("/api/creatures/" + owlbear), DM).andExpect(status().isNoContent());
        as(get("/api/creatures/" + owlbear), DM).andExpect(status().isNotFound());

        as(delete("/api/documents/" + document), DM).andExpect(status().isNoContent());
        as(get("/api/documents/" + document), DM).andExpect(status().isNotFound());
        as(get("/api/spells/" + spell), DM).andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("select count(*) from open5e.spells where document_key = ?", Integer.class, document));
        as(get("/api/creatures/" + other), DM).andExpect(status().isOk());
    }

    @Test
    void updatesDocuments() throws Exception {
        String document = createDocument(DM, "Draft");

        send(put("/api/documents/" + document), DM, "{\"name\": \"Final\", \"desc\": \"Done\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value(document))
                .andExpect(jsonPath("$.name").value("Final"))
                .andExpect(jsonPath("$.desc").value("Done"))
                .andExpect(jsonPath("$.ownerId").value(userService.findOrCreate(DM).id()));
        send(put("/api/documents/srd-2024"), DM, "{\"name\": \"Mine\"}").andExpect(status().isForbidden());
        send(post("/api/documents"), DM, "{\"name\": \"Draft\", \"slug\": \"final\"}").andExpect(status().isCreated());
        send(post("/api/documents"), DM, "{\"name\": \"Final\"}").andExpect(status().isConflict());
    }

    private String createDocument(String user, String name) throws Exception {
        String body = send(post("/api/documents"), user, "{\"name\": \"%s\"}".formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private String createCreature(String user, String name) throws Exception {
        String body = send(post("/api/creatures"), user, "{\"name\": \"%s\"}".formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private String createIn(String document, String user, String path, String name) throws Exception {
        String body = send(post(path), user, "{\"document\": \"%s\", \"name\": \"%s\"}".formatted(document, name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private String copy(String path, String user) throws Exception {
        String body = as(post(path + "/copy"), user)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.key");
    }

    private ResultActions share(String document, String username, String role) throws Exception {
        return send(put("/api/documents/%s/members/%s".formatted(document, username)), DM, "{\"role\": \"%s\"}".formatted(role));
    }

    private ResultActions send(MockHttpServletRequestBuilder request, String user, String json) throws Exception {
        return as(request.contentType(MediaType.APPLICATION_JSON).content(json), user);
    }

    private ResultActions as(MockHttpServletRequestBuilder request, String user) throws Exception {
        return mvc.perform(request.header(DevCurrentUser.HEADER, user));
    }
}
