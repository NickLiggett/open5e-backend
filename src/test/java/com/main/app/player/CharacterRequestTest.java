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
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Asking people to play characters, through real requests as different {@code dev} users. The mail server is a mock;
 * a person can answer only with the address the request was sent to <em>verified</em> on their account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CharacterRequestTest {

    static final String DM = TestUsers.PREFIX + "reqdm";
    static final String ANNA = TestUsers.PREFIX + "reqanna";
    static final String BEN = TestUsers.PREFIX + "reqben";
    static final String ANNA_EMAIL = "zz-test-anna@example.com";
    static final String BEN_EMAIL = "zz-test-ben@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @MockitoBean
    private CharacterRequestMailer mailer;

    private long thorin;

    @BeforeEach
    void setUp() throws Exception {
        TestUsers.cleanUp(jdbc);
        userService.findOrCreate(DM);
        userService.findOrCreate(ANNA);
        userService.findOrCreate(BEN);
        verifyEmail(ANNA, ANNA_EMAIL);
        verifyEmail(BEN, BEN_EMAIL);
        thorin = createCharacter("Thorin");
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void asksAnAddressAndEmailsItOnce() throws Exception {
        ask(DM, thorin, "  " + ANNA_EMAIL.toUpperCase() + " ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ANNA_EMAIL))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.characterName").value("Thorin"))
                .andExpect(jsonPath("$.emailSent").value(true));
        verify(mailer, times(1)).send(ANNA_EMAIL, DM, "Thorin");

        // nobody is the player until they accept
        assertThat(playedBy(thorin)).isNull();

        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.emailSent").value(false));
        verify(mailer, times(1)).send(any(), any(), any());
    }

    @Test
    void askingAnotherAddressReplacesTheRequest() throws Exception {
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk());
        ask(DM, thorin, BEN_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.emailSent").value(true));

        assertThat(jdbc.queryForList("select email from open5e.character_requests where player_id = ?", String.class, thorin))
                .containsExactly(BEN_EMAIL);
        verify(mailer).send(BEN_EMAIL, DM, "Thorin");
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        received(BEN).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void keepsTheRequestWhenTheMailCannotBeSent() throws Exception {
        doThrow(new MailSendException("down")).when(mailer).send(any(), any(), any());

        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.emailSent").value(false));

        received(ANNA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refusesWhatIsNotAnAddressOrNotYoursOrYourOwn() throws Exception {
        ask(DM, thorin, "not-an-address").andExpect(status().isBadRequest());
        ask(DM, thorin, null).andExpect(status().isBadRequest());
        ask(ANNA, thorin, BEN_EMAIL).andExpect(status().isNotFound()); // not hers: not found, as for anything not visible
        verifyEmail(DM, "zz-test-dm@example.com");
        ask(DM, thorin, "ZZ-test-dm@example.com").andExpect(status().isBadRequest());

        verify(mailer, never()).send(any(), any(), any());
        assertThat(jdbc.queryForObject("select count(*) from open5e.character_requests where player_id = ?", Integer.class, thorin)).isZero();
    }

    @Test
    void limitsHowManyAreWaitingAtOnce() throws Exception {
        for (int i = 1; i <= CharacterRequestService.MAX_PENDING; i++) {
            ask(DM, createCharacter("Hero " + i), "zz-test-pending" + i + "@example.com").andExpect(status().isOk());
        }
        long one = createCharacter("One too many");

        ask(DM, one, "zz-test-pending-extra@example.com").andExpect(status().isBadRequest());
        // a character with no request yet is one more
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isBadRequest());
        jdbc.update("update open5e.character_requests set status = 'DENIED', responded_at = now() where email = 'zz-test-pending1@example.com'");
        ask(DM, one, "zz-test-pending-extra@example.com").andExpect(status().isOk());
    }

    @Test
    void listsWhatWasSentNewestFirst() throws Exception {
        long aria = createCharacter("Aria");
        ask(DM, thorin, ANNA_EMAIL);
        ask(DM, aria, BEN_EMAIL);

        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, DM))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].characterName").value("Aria"))
                .andExpect(jsonPath("$[1].characterName").value("Thorin"));
        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void onlyTheVerifiedAddressSeesAndAnswersIt() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);
        long id = requestId(thorin);

        received(ANNA)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].characterName").value("Thorin"))
                .andExpect(jsonPath("$[0].owner").value(DM));
        received(BEN).andExpect(jsonPath("$.length()").value(0));
        received(DM).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/character-requests/" + id + "/accept").header(DevCurrentUser.HEADER, BEN)).andExpect(status().isNotFound());
        mvc.perform(post("/api/character-requests/" + id + "/deny").header(DevCurrentUser.HEADER, BEN)).andExpect(status().isNotFound());
        mvc.perform(post("/api/character-requests/" + id + "/accept").header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());

        // the same address, not verified, counts for nothing
        jdbc.update("update open5e.users set verified_email = null where username = ?", ANNA);
        jdbc.update("update open5e.users set verified_email = null where username = ?", BEN);
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/character-requests/" + id + "/accept").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNotFound());
        assertThat(playedBy(thorin)).isNull();
    }

    @Test
    void acceptingMakesThemThePlayer() throws Exception {
        int version = version(thorin);
        ask(DM, thorin, ANNA_EMAIL);

        mvc.perform(post("/api/character-requests/" + requestId(thorin) + "/accept").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(status().isNoContent());

        assertThat(playedBy(thorin)).isEqualTo(ANNA);
        assertThat(version(thorin)).isEqualTo(version + 1);
        assertThat(jdbc.queryForObject("select count(*) from open5e.character_requests", Integer.class)).isZero();
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, ANNA))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Thorin"))
                .andExpect(jsonPath("$[0].role").value("PLAYER"));
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void denyingIsKeptAndTheSameAddressCannotBeAskedAgainAtOnce() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);
        long id = requestId(thorin);

        mvc.perform(post("/api/character-requests/" + id + "/deny").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNoContent());

        assertThat(playedBy(thorin)).isNull();
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/character-requests/" + id + "/accept").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNotFound());
        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, DM))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("DENIED"))
                .andExpect(jsonPath("$[0].respondedAt").exists())
                .andExpect(jsonPath("$[0].askAgainAt").exists());

        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isConflict());
        verify(mailer, times(1)).send(any(), any(), any());

        ask(DM, thorin, BEN_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")); // someone else is fine

        // ... and after the wait the same one can be asked again
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk());
        mvc.perform(post("/api/character-requests/" + requestId(thorin) + "/deny").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNoContent());
        jdbc.update("update open5e.character_requests set responded_at = now() - interval '8 days'");
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.emailSent").value(true));
        received(ANNA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void theOwnerWithdrawsOrDismissesIt() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);
        long id = requestId(thorin);

        mvc.perform(delete("/api/character-requests/" + id).header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/character-requests/" + id).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/character-requests/" + id).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNotFound());

        ask(DM, thorin, ANNA_EMAIL);
        mvc.perform(post("/api/character-requests/" + requestId(thorin) + "/deny").header(DevCurrentUser.HEADER, ANNA));
        mvc.perform(delete("/api/character-requests/" + requestId(thorin)).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());
        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anExpiredRequestIsHiddenAndCannotBeAccepted() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);
        long id = requestId(thorin);
        jdbc.update("update open5e.character_requests set expires_at = now() - interval '1 day'");

        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/character-requests/sent").header(DevCurrentUser.HEADER, DM)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/character-requests/" + id + "/accept").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNotFound());
        assertThat(playedBy(thorin)).isNull();

        // the owner can ask the same address again
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk()).andExpect(jsonPath("$.emailSent").value(true));
        received(ANNA).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void theOwnerSettlingItThemselfEndsTheRequest() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/players/" + thorin)
                        .header(DevCurrentUser.HEADER, DM).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Thorin\", \"ruleset\": \"5e-2014\", \"playedBy\": \"" + DM + "\"}"))
                .andExpect(status().isOk());

        received(ANNA).andExpect(jsonPath("$.length()").value(0));
        assertThat(jdbc.queryForObject("select count(*) from open5e.character_requests", Integer.class)).isZero();
    }

    @Test
    void deletingTheCharacterDeletesItsRequest() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);

        mvc.perform(delete("/api/players/" + thorin).header(DevCurrentUser.HEADER, DM)).andExpect(status().isNoContent());

        assertThat(jdbc.queryForObject("select count(*) from open5e.character_requests", Integer.class)).isZero();
        received(ANNA).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void whoAcceptedCanStepDown() throws Exception {
        ask(DM, thorin, ANNA_EMAIL);
        mvc.perform(post("/api/character-requests/" + requestId(thorin) + "/accept").header(DevCurrentUser.HEADER, ANNA));

        mvc.perform(post("/api/players/" + thorin + "/step-down").header(DevCurrentUser.HEADER, ANNA)).andExpect(status().isNoContent());

        assertThat(playedBy(thorin)).isNull();
        mvc.perform(get("/api/players").header(DevCurrentUser.HEADER, ANNA)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/players/" + thorin).header(DevCurrentUser.HEADER, DM)).andExpect(status().isOk());
        ask(DM, thorin, ANNA_EMAIL).andExpect(status().isOk()); // the owner can ask again
    }

    private ResultActions ask(String user, long id, String email) throws Exception {
        return mvc.perform(post("/api/players/" + id + "/requests").header(DevCurrentUser.HEADER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(email == null ? "{}" : "{\"email\": \"" + email + "\"}"));
    }

    private ResultActions received(String user) throws Exception {
        return mvc.perform(get("/api/character-requests/received").header(DevCurrentUser.HEADER, user))
                .andExpect(status().isOk());
    }

    private long createCharacter(String name) throws Exception {
        String response = mvc.perform(post("/api/players").header(DevCurrentUser.HEADER, DM)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"ruleset\": \"5e-2014\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private void verifyEmail(String user, String email) {
        jdbc.update("update open5e.users set verified_email = ? where username = ?", email, user);
    }

    private long requestId(long player) {
        return jdbc.queryForObject("select id from open5e.character_requests where player_id = ?", Long.class, player);
    }

    private String playedBy(long player) {
        return jdbc.queryForList("select u.username from open5e.player_characters p join open5e.users u on u.id = p.played_by where p.id = ?",
                String.class, player).stream().findFirst().orElse(null);
    }

    private int version(long player) {
        return jdbc.queryForObject("select version from open5e.player_characters where id = ?", Integer.class, player);
    }
}
