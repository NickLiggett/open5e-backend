package com.main.app.document;

import com.jayway.jsonpath.JsonPath;
import com.main.app.common.TestUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sharing by email address, through real requests with bearer tokens (the default profile). A DM owns a document and
 * invites addresses; people accept by signing in with that address <em>verified</em>. The mail server is a mock.
 */
@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/no-keys-in-tests")
@AutoConfigureMockMvc
class InvitationTest {

    static final String ISSUER = "http://localhost:8180/realms/open5e";
    static final String DM = TestUsers.PREFIX + "dm";
    static final String PLAYER_EMAIL = "zz-test-player@example.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private InvitationMailer mailer;

    private String key;

    @BeforeEach
    void createDocument() throws Exception {
        TestUsers.cleanUp(jdbc);
        String body = mvc.perform(post("/api/documents").with(token("sub-dm", DM, "zz-test-dm@example.com", true))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Invitation Test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        key = JsonPath.read(body, "$.key");
    }

    @AfterEach
    void cleanUp() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void emailsAnAddressAndKeepsAPendingInvitation() throws Exception {
        invite(PLAYER_EMAIL.toUpperCase(), "editor")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(PLAYER_EMAIL))
                .andExpect(jsonPath("$.role").value("EDITOR"))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.emailSent").value(true));

        verify(mailer).send(eq(PLAYER_EMAIL), eq(DM), eq("Invitation Test"), eq("EDITOR"));
        mvc.perform(get("/api/documents/" + key + "/invitations").with(dm()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(PLAYER_EMAIL))
                .andExpect(jsonPath("$[0].role").value("EDITOR"))
                .andExpect(jsonPath("$[0].invitedBy").value(DM));
    }

    @Test
    void invitingAgainChangesTheRoleAndSendsItAgain() throws Exception {
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());
        invite(PLAYER_EMAIL, "EDITOR").andExpect(status().isOk());

        assertThat(pending()).containsExactly(PLAYER_EMAIL + " EDITOR");
        verify(mailer, org.mockito.Mockito.times(2)).send(eq(PLAYER_EMAIL), any(), any(), any());
    }

    @Test
    void keepsTheInvitationWhenTheMailServerIsDown() throws Exception {
        doThrow(new MailSendException("connection refused")).when(mailer).send(any(), any(), any(), any());

        invite(PLAYER_EMAIL, "VIEWER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailSent").value(false));

        assertThat(pending()).containsExactly(PLAYER_EMAIL + " VIEWER");
    }

    @Test
    void signingInWithTheVerifiedAddressAcceptsIt() throws Exception {
        invite(PLAYER_EMAIL, "EDITOR").andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + key).with(token("sub-other", TestUsers.PREFIX + "other", "o@example.com", true)))
                .andExpect(status().isNotFound()); // another person, signing in with another address

        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());

        mvc.perform(get("/api/documents/" + key).with(player(true))).andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + key + "/members").with(dm()))
                .andExpect(jsonPath("$[?(@.username == '" + TestUsers.PREFIX + "player')].role").value("EDITOR"));
        assertThat(pending()).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from open5e.document_invitations where email = ?",
                Integer.class, PLAYER_EMAIL)).isZero();
    }

    @Test
    void acceptsAnInvitationOnTheFirstRequestOfAnExistingUserWhoVerifiesLater() throws Exception {
        mvc.perform(get("/api/me").with(player(false))).andExpect(status().isOk());
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk()); // not verified yet: still an invitation
        mvc.perform(get("/api/documents/" + key).with(player(false))).andExpect(status().isNotFound());

        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());

        mvc.perform(get("/api/documents/" + key).with(player(true))).andExpect(status().isOk());
    }

    @Test
    void anAddressTheSignInServiceHasNotVerifiedAcceptsNothing() throws Exception {
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());

        mvc.perform(get("/api/me").with(player(false))).andExpect(status().isOk());
        mvc.perform(get("/api/me").with(token("sub-player", TestUsers.PREFIX + "player", PLAYER_EMAIL, null)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/documents/" + key).with(player(false))).andExpect(status().isNotFound());
        assertThat(pending()).containsExactly(PLAYER_EMAIL + " VIEWER");
    }

    @Test
    void anExpiredInvitationIsNotAcceptedOrListed() throws Exception {
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());
        jdbc.update("update open5e.document_invitations set expires_at = now() - interval '1 day'");

        mvc.perform(get("/api/documents/" + key + "/invitations").with(dm())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());

        mvc.perform(get("/api/documents/" + key).with(player(true))).andExpect(status().isNotFound());
    }

    @Test
    void acceptingNeverLowersARoleTheyAlreadyHave() throws Exception {
        mvc.perform(get("/api/me").with(player(false))).andExpect(status().isOk());
        jdbc.update("""
                insert into open5e.document_members (document_key, user_id, role)
                select ?, id, 'EDITOR' from open5e.users where username = ?""", key, TestUsers.PREFIX + "player");
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());

        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());

        mvc.perform(get("/api/documents/" + key + "/members").with(dm()))
                .andExpect(jsonPath("$[?(@.username == '" + TestUsers.PREFIX + "player')].role").value("EDITOR"));
    }

    @Test
    void addsAUserWhoAlreadyHasTheVerifiedAddressStraightAway() throws Exception {
        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());

        invite(PLAYER_EMAIL, "VIEWER")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TestUsers.PREFIX + "player"))
                .andExpect(jsonPath("$.emailSent").value(false));

        verify(mailer, never()).send(any(), any(), any(), any());
        assertThat(pending()).isEmpty();
        mvc.perform(get("/api/documents/" + key).with(player(true))).andExpect(status().isOk());
    }

    @Test
    void youCannotInviteYourself() throws Exception {
        invite("zz-test-dm@example.com", "VIEWER").andExpect(status().isBadRequest());
    }

    @Test
    void cancelsAnInvitation() throws Exception {
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());
        long id = jdbc.queryForObject("select id from open5e.document_invitations where email = ?", Long.class, PLAYER_EMAIL);

        mvc.perform(delete("/api/documents/" + key + "/invitations/" + id).with(dm())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/documents/" + key + "/invitations/" + id).with(dm())).andExpect(status().isNotFound());

        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + key).with(player(true))).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheOwnerManagesInvitations() throws Exception {
        invite(PLAYER_EMAIL, "VIEWER").andExpect(status().isOk());
        long id = jdbc.queryForObject("select id from open5e.document_invitations where email = ?", Long.class, PLAYER_EMAIL);
        RequestPostProcessor stranger = token("sub-stranger", TestUsers.PREFIX + "stranger", "zz-test-s@example.com", true);

        mvc.perform(get("/api/documents/" + key + "/invitations").with(stranger)).andExpect(status().isNotFound());
        mvc.perform(post("/api/documents/" + key + "/invitations").with(stranger).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"zz-test-x@example.com\", \"role\": \"VIEWER\"}")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/documents/" + key + "/invitations/" + id).with(stranger)).andExpect(status().isNotFound());
        mvc.perform(get("/api/documents/" + key + "/invitations")).andExpect(status().isUnauthorized());

        // someone who can see the document, but doesn't own it
        mvc.perform(get("/api/me").with(player(true))).andExpect(status().isOk());
        mvc.perform(get("/api/documents/" + key + "/invitations").with(player(true))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/documents/" + key + "/invitations/" + id).with(player(true))).andExpect(status().isForbidden());
        assertThat(pending()).isEmpty(); // the player signing in accepted it, which deleted it
    }

    @Test
    void refusesNonsense() throws Exception {
        invite("not an email", "VIEWER").andExpect(status().isBadRequest());
        invite("two@@example.com", "VIEWER").andExpect(status().isBadRequest());
        invite("a@b", "VIEWER").andExpect(status().isBadRequest());
        invite(PLAYER_EMAIL, "OWNER").andExpect(status().isBadRequest());
        invite(PLAYER_EMAIL, null).andExpect(status().isBadRequest());
        mvc.perform(post("/api/documents/" + key + "/invitations").with(dm()).contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isBadRequest());
        assertThat(pending()).isEmpty();
    }

    @Test
    void limitsPendingInvitations() throws Exception {
        for (int i = 0; i < InvitationService.MAX_PENDING; i++) {
            invite("zz-test-" + i + "@example.com", "VIEWER").andExpect(status().isOk());
        }

        invite("zz-test-one-too-many@example.com", "VIEWER").andExpect(status().isBadRequest());
        invite("zz-test-0@example.com", "EDITOR").andExpect(status().isOk()); // changing one is fine
    }

    private ResultActions invite(String email, String role) throws Exception {
        String body = "{\"email\": \"%s\", \"role\": %s}".formatted(email, role == null ? "null" : "\"" + role + "\"");
        return mvc.perform(post("/api/documents/" + key + "/invitations").with(dm())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> pending() {
        return jdbc.queryForList("select email || ' ' || role from open5e.document_invitations where document_key = ? order by email",
                String.class, key);
    }

    private static RequestPostProcessor dm() {
        return token("sub-dm", DM, "zz-test-dm@example.com", true);
    }

    private static RequestPostProcessor player(boolean verified) {
        return token("sub-player", TestUsers.PREFIX + "player", PLAYER_EMAIL, verified);
    }

    private static RequestPostProcessor token(String subject, String username, String email, Boolean verified) {
        return jwt().jwt(jwt -> {
            jwt.subject(subject).issuer(ISSUER).claim("preferred_username", username).claim("email", email);
            if (verified != null) {
                jwt.claim("email_verified", verified);
            }
        });
    }
}
