package com.main.app.importer;

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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What private content means for the people who use the app, through real requests: it is the owner's own document, so
 * nobody else sees it, signed in or not, until the owner shares it like any of their documents. Unlike the merge tests,
 * this one applies the content for real (the rows are committed) and removes it afterwards.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class PrivateContentVisibilityTest {

    static final String OWNER = "zz-test-owner";
    static final String FRIEND = "zz-test-friend";
    static final String STRANGER = "zz-test-stranger";
    static final String SPECIES = "/api/species/zz-test-private_x";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @Autowired
    private DefaultContentImporter importer;

    @Autowired
    private JsonMapper mapper;

    @BeforeEach
    void setUp() {
        TestUsers.cleanUp(jdbc);
        for (String user : List.of(OWNER, FRIEND, STRANGER)) {
            userService.findOrCreate(user);
        }
        new PrivateContentImporter(ContentFixtures.content(mapper, null, ContentFixtures.PRIVATE), importer, jdbc).run(true, false);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void theOwnerSeesItAndNobodyElseDoes() throws Exception {
        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, OWNER)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Xander"));

        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, FRIEND)).andExpect(status().isNotFound());
        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
        mvc.perform(get(SPECIES)).andExpect(status().isNotFound()); // no header: the default dev user, who isn't the owner either
    }

    @Test
    void itIsNotInAnyonesSearchResultsButTheOwners() throws Exception {
        mvc.perform(get("/api/species").param("name", "Xander").header(DevCurrentUser.HEADER, OWNER))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mvc.perform(get("/api/species").param("name", "Xander").header(DevCurrentUser.HEADER, STRANGER))
                .andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/species").param("name", "Yara")).andExpect(jsonPath("$.page.totalElements").value(0));
        mvc.perform(get("/api/documents/zz-test-private").header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
    }

    @Test
    void theOwnerSharesItLikeAnyDocument_andThenTheFriendSeesItButNotTheStranger() throws Exception {
        mvc.perform(put("/api/documents/zz-test-private/members/" + FRIEND).header(DevCurrentUser.HEADER, OWNER)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\": \"VIEWER\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, FRIEND)).andExpect(status().isOk());
        mvc.perform(get("/api/species").param("name", "Yara").header(DevCurrentUser.HEADER, FRIEND))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, STRANGER)).andExpect(status().isNotFound());
    }

    @Test
    void aViewerCannotChangeIt_andAnEditorCan() throws Exception {
        jdbc.update("insert into open5e.document_members (document_key, user_id, role) select 'zz-test-private', id, 'VIEWER' from open5e.users where username = ?", FRIEND);

        mvc.perform(put(SPECIES).header(DevCurrentUser.HEADER, FRIEND).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Mine now\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void applyingItAgainKeepsWhoItIsSharedWith() throws Exception {
        jdbc.update("insert into open5e.document_members (document_key, user_id, role) select 'zz-test-private', id, 'VIEWER' from open5e.users where username = ?", FRIEND);

        List<ImportReport> again = new PrivateContentImporter(ContentFixtures.content(mapper, null, ContentFixtures.PRIVATE), importer, jdbc).run(true, false);

        assertThat(again.getFirst().changes()).isZero();
        mvc.perform(get(SPECIES).header(DevCurrentUser.HEADER, FRIEND)).andExpect(status().isOk());
    }

    @Test
    void itIsNotDefaultContent_soAFullImportOfOpen5eWouldNeverPruneIt() {
        // Default content is what an import prunes when Open5e doesn't have it; this is a user's document.
        assertThat(jdbc.queryForObject("select owner_id is not null from open5e.documents where key = 'zz-test-private'", Boolean.class)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from open5e.species where key like 'zz-test-private%' and document_key in "
                + "(select key from open5e.documents where owner_id is null)", Integer.class)).isZero();
    }
}
