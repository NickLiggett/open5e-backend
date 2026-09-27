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
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every writable endpoint, found from the controllers: copy a default resource, check the copy has the original's
 * content, send the copy's full JSON back with {@code PUT} (every nested shape must be accepted as it is returned),
 * change it with {@code PATCH}, and delete it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WriteSmokeTest {

    static final String USER = TestUsers.PREFIX + "writer";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        TestUsers.cleanUp(jdbc);
        userService.findOrCreate(USER);
    }

    @AfterEach
    void tearDown() {
        TestUsers.cleanUp(jdbc);
    }

    @Test
    void everyWritableResourceCanBeCopiedChangedAndDeleted() throws Exception {
        List<String> paths = new ArrayList<>();
        for (Object controller : context.getBeansWithAnnotation(RestController.class).values()) {
            Class<?> type = ClassUtils.getUserClass(controller);
            boolean copyable = Arrays.stream(type.getMethods()).anyMatch(method -> {
                PostMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, PostMapping.class);
                return mapping != null && Arrays.asList(mapping.value()).contains("/{key}/copy");
            });
            if (copyable) {
                paths.add(AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class).value()[0]);
            }
        }
        assertThat(paths).hasSize(28);

        for (String path : paths.stream().sorted().toList()) {
            JsonNode original = json(mvc.perform(get(path).param("pageSize", "1")).andReturn()).get("content").get(0);
            String originalKey = original.get("key").asString();

            JsonNode copy = json(mvc.perform(post(path + "/" + originalKey + "/copy").header(DevCurrentUser.HEADER, USER))
                    .andExpect(status().isCreated()).andReturn());
            String key = copy.get("key").asString();
            assertEquals(originalKey, copy.get("derivedFrom").asString(), path);
            assertEquals(withoutManagedFields(original), withoutManagedFields(copy), path + ": copy differs from original");

            JsonNode replaced = json(mvc.perform(put(path + "/" + key).header(DevCurrentUser.HEADER, USER)
                            .contentType(MediaType.APPLICATION_JSON).content(copy.toString()))
                    .andExpect(status().isOk()).andReturn());
            assertEquals(copy, replaced, path + ": PUT of the fetched JSON changed it");

            String field = copy.has("name") ? "name" : "shortName";
            JsonNode changed = json(mvc.perform(patch(path + "/" + key).header(DevCurrentUser.HEADER, USER)
                            .contentType(MediaType.APPLICATION_JSON).content("{\"" + field + "\": \"Changed\"}"))
                    .andExpect(status().isOk()).andReturn());
            assertEquals("Changed", changed.get(field).asString(), path);

            mvc.perform(delete(path + "/" + key).header(DevCurrentUser.HEADER, USER)).andExpect(status().isNoContent());
            mvc.perform(get(path + "/" + key).header(DevCurrentUser.HEADER, USER)).andExpect(status().isNotFound());
        }
    }

    private JsonNode json(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private static JsonNode withoutManagedFields(JsonNode resource) {
        ObjectNode copy = (ObjectNode) resource.deepCopy();
        ResourceType.MANAGED_FIELDS.forEach(copy::remove);
        return copy;
    }
}
