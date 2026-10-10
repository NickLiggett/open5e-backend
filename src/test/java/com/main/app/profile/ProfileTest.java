package com.main.app.profile;

import com.main.app.common.TestUsers;
import com.main.app.user.DevCurrentUser;
import com.main.app.user.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A user's own settings, avatar picture and tracker state, through real requests as different {@code dev} users. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProfileTest {

    static final String DM = TestUsers.PREFIX + "dm";
    static final String PLAYER = TestUsers.PREFIX + "player";

    static final byte[] PNG = bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4);
    static final byte[] JPEG = bytes(0xFF, 0xD8, 0xFF, 0xE0, 0, 16, 'J', 'F', 'I', 'F');
    static final byte[] GIF = bytes('G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0);
    static final byte[] WEBP = bytes('R', 'I', 'F', 'F', 4, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' ');

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
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    // ----- settings

    @Test
    void settingsStartWithNothingChosen() throws Exception {
        as(get("/api/me/settings"), DM)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").doesNotExist())
                .andExpect(jsonPath("$.avatarVersion").value(nullValue()));
    }

    @Test
    void keepsTheSettingsChosen() throws Exception {
        json(put("/api/me/settings"), DM, """
                {"mode": "dark", "primary": "#2E7D32", "secondary": "#8d6e63"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("dark"))
                .andExpect(jsonPath("$.primary").value("#2e7d32"));

        as(get("/api/me/settings"), DM)
                .andExpect(jsonPath("$.mode").value("dark"))
                .andExpect(jsonPath("$.primary").value("#2e7d32"))
                .andExpect(jsonPath("$.secondary").value("#8d6e63"));
    }

    @Test
    void replacesTheSettingsRatherThanMergingThem() throws Exception {
        json(put("/api/me/settings"), DM, "{\"mode\": \"dark\", \"primary\": \"#112233\"}").andExpect(status().isOk());
        json(put("/api/me/settings"), DM, "{\"primary\": \"#445566\"}")
                .andExpect(jsonPath("$.primary").value("#445566"))
                .andExpect(jsonPath("$.mode").doesNotExist());
    }

    @Test
    void keepsEachUsersSettingsSeparately() throws Exception {
        json(put("/api/me/settings"), DM, "{\"mode\": \"dark\"}").andExpect(status().isOk());
        json(put("/api/me/settings"), PLAYER, "{\"mode\": \"light\"}").andExpect(status().isOk());

        as(get("/api/me/settings"), DM).andExpect(jsonPath("$.mode").value("dark"));
        as(get("/api/me/settings"), PLAYER).andExpect(jsonPath("$.mode").value("light"));
    }

    @Test
    void ignoresTheAvatarVersionAndSettingsLeftNull() throws Exception {
        json(put("/api/me/settings"), DM, "{\"avatarVersion\": 5, \"mode\": null, \"primary\": \"#112233\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarVersion").value(nullValue()))
                .andExpect(jsonPath("$.mode").doesNotExist())
                .andExpect(jsonPath("$.primary").value("#112233"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"mode\": \"neon\"}",
            "{\"mode\": 3}",
            "{\"primary\": \"blue\"}",
            "{\"primary\": \"#12345\"}",
            "{\"secondary\": \"#1234567\"}",
            "{\"secondary\": 5}",
    })
    void refusesSettingsThatAreNotAllowed(String body) throws Exception {
        json(put("/api/me/settings"), DM, body).andExpect(status().isBadRequest());
        as(get("/api/me/settings"), DM).andExpect(jsonPath("$.mode").doesNotExist()); // nothing was kept
    }

    @Test
    void refusesASettingThatIsNotOne() throws Exception {
        json(put("/api/me/settings"), DM, "{\"theme\": \"Forest\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown setting 'theme'"));
    }

    // ----- tracker

    @Test
    void theTrackerStartsEmpty() throws Exception {
        as(get("/api/me/tracker"), DM).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void keepsWhateverStateTheAppSaves() throws Exception {
        json(put("/api/me/tracker"), DM, """
                {"version": 1, "round": 3, "combatants": [
                  {"id": 1, "name": "Gribble", "initiative": 18, "hp": 22, "creatureKey": "a5e-mm_aboleth", "turn": true},
                  {"id": 2, "name": "Snarl", "initiative": 7, "tags": ["a", "b"], "nothing": null}]}""")
                .andExpect(status().isOk());

        as(get("/api/me/tracker"), DM)
                .andExpect(jsonPath("$.round").value(3))
                .andExpect(jsonPath("$.combatants.length()").value(2))
                .andExpect(jsonPath("$.combatants[0].name").value("Gribble"))
                .andExpect(jsonPath("$.combatants[0].turn").value(true))
                .andExpect(jsonPath("$.combatants[1].tags[1]").value("b"))
                .andExpect(jsonPath("$.combatants[1].nothing").value(nullValue()));
    }

    @Test
    void replacesTheTrackerStateAndKeepsEachUsersSeparately() throws Exception {
        json(put("/api/me/tracker"), DM, "{\"round\": 1, \"combatants\": [{\"id\": 1}]}").andExpect(status().isOk());
        json(put("/api/me/tracker"), DM, "{\"round\": 2}").andExpect(status().isOk());
        json(put("/api/me/tracker"), PLAYER, "{\"round\": 9}").andExpect(status().isOk());

        as(get("/api/me/tracker"), DM)
                .andExpect(jsonPath("$.round").value(2))
                .andExpect(jsonPath("$.combatants").doesNotExist());
        as(get("/api/me/tracker"), PLAYER).andExpect(jsonPath("$.round").value(9));
    }

    @Test
    void refusesATrackerStateThatIsTooBig() throws Exception {
        String big = "{\"notes\": \"" + "x".repeat(ProfileService.MAX_TRACKER_CHARS) + "\"}";

        json(put("/api/me/tracker"), DM, big)
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.detail", containsString("too big")));
        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1, 2]", "\"text\"", "42", "not json"})
    void refusesATrackerStateThatIsNotAnObject(String body) throws Exception {
        json(put("/api/me/tracker"), DM, body).andExpect(status().isBadRequest());
    }

    // ----- saved encounters

    @Test
    void theSavedEncountersStartEmpty() throws Exception {
        as(get("/api/me/encounters"), DM).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void keepsWhateverTheAppSaves() throws Exception {
        json(put("/api/me/encounters"), DM, """
                {"version": 1, "encounters": [
                  {"id": 1, "name": "Goblin ambush", "ruleset": "5e-2024", "monsters": [{"key": "srd_goblin", "count": 4}], "extras": [3, 3]}]}""")
                .andExpect(status().isOk());

        as(get("/api/me/encounters"), DM)
                .andExpect(jsonPath("$.encounters.length()").value(1))
                .andExpect(jsonPath("$.encounters[0].name").value("Goblin ambush"))
                .andExpect(jsonPath("$.encounters[0].monsters[0].count").value(4))
                .andExpect(jsonPath("$.encounters[0].extras[1]").value(3));
    }

    @Test
    void replacesTheSavedEncountersAndKeepsEachUsersSeparately_andApartFromTheTracker() throws Exception {
        json(put("/api/me/tracker"), DM, "{\"round\": 4}").andExpect(status().isOk());
        json(put("/api/me/encounters"), DM, "{\"encounters\": [{\"id\": 1}, {\"id\": 2}]}").andExpect(status().isOk());
        json(put("/api/me/encounters"), DM, "{\"encounters\": [{\"id\": 3}]}").andExpect(status().isOk());
        json(put("/api/me/encounters"), PLAYER, "{\"encounters\": []}").andExpect(status().isOk());

        as(get("/api/me/encounters"), DM).andExpect(jsonPath("$.encounters.length()").value(1)).andExpect(jsonPath("$.encounters[0].id").value(3));
        as(get("/api/me/encounters"), PLAYER).andExpect(jsonPath("$.encounters.length()").value(0));
        as(get("/api/me/tracker"), DM).andExpect(jsonPath("$.round").value(4)).andExpect(jsonPath("$.encounters").doesNotExist());
    }

    @Test
    void refusesSavedEncountersThatAreTooBig() throws Exception {
        String big = "{\"notes\": \"" + "x".repeat(ProfileService.MAX_ENCOUNTERS_CHARS) + "\"}";

        json(put("/api/me/encounters"), DM, big)
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.detail", containsString("too big")));
        as(get("/api/me/encounters"), DM).andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1, 2]", "\"text\"", "42", "not json"})
    void refusesSavedEncountersThatAreNotAnObject(String body) throws Exception {
        json(put("/api/me/encounters"), DM, body).andExpect(status().isBadRequest());
    }

    // ----- avatar

    @Test
    void servesTheAvatarToEveryoneByUsername() throws Exception {
        long version = saveAvatar(DM, "image/png", PNG);

        // asked for by someone else, as a browser <img> would: no token, and no say in who they are
        MockHttpServletResponse response = as(get("/api/users/" + DM + "/avatar"), PLAYER)
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("ETag", "\"" + version + "\""))
                .andExpect(header().string("Cache-Control", "no-cache"))
                .andReturn().getResponse();
        assertThat(response.getContentAsByteArray()).isEqualTo(PNG);

        as(get("/api/me/settings"), DM).andExpect(jsonPath("$.avatarVersion").value(version));
    }

    @Test
    void answersNotModifiedForAPictureThatHasNotChanged() throws Exception {
        long version = saveAvatar(DM, "image/png", PNG);

        as(get("/api/users/" + DM + "/avatar").header("If-None-Match", "\"" + version + "\""), PLAYER)
                .andExpect(status().isNotModified());
        as(get("/api/users/" + DM + "/avatar").header("If-None-Match", "\"1\""), PLAYER)
                .andExpect(status().isOk());
    }

    @Test
    void acceptsEachKindOfPictureAndReplacesTheOldOne() throws Exception {
        for (Object[] picture : new Object[][]{{"image/png", PNG}, {"image/jpeg", JPEG}, {"image/gif", GIF}, {"image/webp", WEBP}}) {
            saveAvatar(DM, (String) picture[0], (byte[]) picture[1]);

            MockHttpServletResponse response = as(get("/api/users/" + DM + "/avatar"), DM)
                    .andExpect(header().string("Content-Type", (String) picture[0])).andReturn().getResponse();
            assertThat(response.getContentAsByteArray()).isEqualTo((byte[]) picture[1]);
        }
        assertThat(jdbc.queryForObject("select count(*) from open5e.user_avatars a join open5e.users u on u.id = a.user_id where u.username = ?",
                Integer.class, DM)).isEqualTo(1);
    }

    @Test
    void eachUserHasTheirOwnAvatar() throws Exception {
        saveAvatar(DM, "image/png", PNG);
        saveAvatar(PLAYER, "image/jpeg", JPEG);

        assertThat(as(get("/api/users/" + DM + "/avatar"), DM).andReturn().getResponse().getContentAsByteArray()).isEqualTo(PNG);
        assertThat(as(get("/api/users/" + PLAYER + "/avatar"), DM).andReturn().getResponse().getContentAsByteArray()).isEqualTo(JPEG);
    }

    @Test
    void findsAUsersAvatarWhateverTheCaseOfTheirName() throws Exception {
        saveAvatar(DM, "image/png", PNG);

        as(get("/api/users/" + DM.toUpperCase() + "/avatar"), PLAYER).andExpect(status().isOk());
    }

    @Test
    void removesTheAvatar() throws Exception {
        saveAvatar(DM, "image/png", PNG);

        as(delete("/api/me/avatar"), DM).andExpect(status().isNoContent());
        as(delete("/api/me/avatar"), DM).andExpect(status().isNoContent()); // nothing to remove is fine

        as(get("/api/users/" + DM + "/avatar"), PLAYER).andExpect(status().isNotFound());
        as(get("/api/me/settings"), DM).andExpect(jsonPath("$.avatarVersion").value(nullValue()));
    }

    @Test
    void hasNoAvatarForAUserWithoutOneOrNoSuchUser() throws Exception {
        as(get("/api/users/" + PLAYER + "/avatar"), DM).andExpect(status().isNotFound());
        as(get("/api/users/" + TestUsers.PREFIX + "nobody/avatar"), DM).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "image/svg+xml", "application/octet-stream", "image/bmp"})
    void refusesFilesThatAreNotOneOfTheAllowedPictureTypes(String type) throws Exception {
        as(put("/api/me/avatar").contentType(type).content(PNG), DM).andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void refusesBytesThatAreNotThePictureTheySay() throws Exception {
        as(put("/api/me/avatar").contentType("image/png").content(JPEG), DM)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("That isn't a PNG picture"));
        as(put("/api/me/avatar").contentType("image/webp").content(PNG), DM).andExpect(status().isBadRequest());
        as(put("/api/me/avatar").contentType("image/png").content(new byte[0]), DM).andExpect(status().isBadRequest());
        as(put("/api/me/avatar").contentType("image/png").content("<svg onload=alert(1)>".getBytes()), DM)
                .andExpect(status().isBadRequest());
        as(get("/api/users/" + DM + "/avatar"), PLAYER).andExpect(status().isNotFound()); // none was kept
    }

    @Test
    void refusesAPictureThatIsTooBig() throws Exception {
        byte[] big = new byte[ProfileService.MAX_AVATAR_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        as(put("/api/me/avatar").contentType("image/png").content(big), DM).andExpect(status().isContentTooLarge());

        byte[] biggestAllowed = new byte[ProfileService.MAX_AVATAR_BYTES];
        System.arraycopy(PNG, 0, biggestAllowed, 0, PNG.length);
        as(put("/api/me/avatar").contentType("image/png").content(biggestAllowed), DM).andExpect(status().isOk());
    }

    @Test
    void acceptsTheTypeWithParametersAndInAnyCase() throws Exception {
        as(put("/api/me/avatar").contentType("IMAGE/Png; name=me.png").content(PNG), DM).andExpect(status().isOk());
    }

    // ----- all together

    @Test
    void deletingAUserDeletesTheirSettingsAvatarTrackerAndEncounters() throws Exception {
        json(put("/api/me/settings"), DM, "{\"mode\": \"dark\"}").andExpect(status().isOk());
        json(put("/api/me/tracker"), DM, "{\"round\": 1}").andExpect(status().isOk());
        json(put("/api/me/encounters"), DM, "{\"encounters\": []}").andExpect(status().isOk());
        saveAvatar(DM, "image/png", PNG);
        long id = userService.findOrCreate(DM).id();
        for (String table : new String[]{"user_settings", "user_tracker_states", "user_encounter_states", "user_avatars"}) {
            assertThat(jdbc.queryForObject("select count(*) from open5e." + table + " where user_id = ?", Integer.class, id)).isEqualTo(1);
        }

        TestUsers.cleanUp(jdbc);

        for (String table : new String[]{"user_settings", "user_tracker_states", "user_encounter_states", "user_avatars"}) {
            assertThat(jdbc.queryForObject("select count(*) from open5e." + table + " where user_id = ?", Integer.class, id)).isZero();
        }
    }

    // ----- helpers

    private long saveAvatar(String user, String type, byte[] image) throws Exception {
        String body = as(put("/api/me/avatar").contentType(type).content(image), user)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll("\\D", ""));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, String user, String json) throws Exception {
        return as(request.contentType(MediaType.APPLICATION_JSON).content(json), user);
    }

    private ResultActions as(MockHttpServletRequestBuilder request, String user) throws Exception {
        return mvc.perform(request.header(DevCurrentUser.HEADER, user));
    }

    private static byte[] bytes(Object... values) {
        byte[] result = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (byte) (values[i] instanceof Character c ? c : (Integer) values[i]);
        }
        return result;
    }
}
