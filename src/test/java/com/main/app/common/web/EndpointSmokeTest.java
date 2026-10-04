package com.main.app.common.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Every {@code /api/<table>} endpoint, found from the controllers: the list's total matches the table's default
 * content, a listed key can be fetched, and an unknown key is a problem-details 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndpointSmokeTest {

    /** Endpoints that aren't a table. */
    private static final Set<String> NOT_RESOURCES = Set.of("/api/me", "/api/users", "/api/players");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext context;

    @Test
    void everyResourceEndpointWorks() throws Exception {
        List<String> paths = new ArrayList<>();
        for (Object controller : context.getBeansWithAnnotation(RestController.class).values()) {
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller.getClass(), RequestMapping.class);
            if (mapping != null && !NOT_RESOURCES.contains(mapping.value()[0])) {
                paths.add(mapping.value()[0]);
            }
        }
        assertThat(paths).hasSizeGreaterThanOrEqualTo(32);

        for (String path : paths.stream().sorted().toList()) {
            String table = path.substring("/api/".length());
            String visible = hasColumn(table, "document_key")
                    ? "document_key in (select key from open5e.documents where owner_id is null)"
                    : hasColumn(table, "owner_id") ? "owner_id is null" : "true";
            int expected = jdbc.queryForObject("select count(*) from open5e." + table + " where " + visible, Integer.class);

            String body = mvc.perform(get(path).param("pageSize", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page.totalElements").value(expected))
                    .andReturn().getResponse().getContentAsString();
            String key = JsonPath.read(body, "$.content[0].key");

            mvc.perform(get(path + "/" + key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.key").value(key));

            mvc.perform(get(path + "/zz-no-such-key"))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                    .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("zz-no-such-key")));
        }
    }

    private boolean hasColumn(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists (select 1 from information_schema.columns
                               where table_schema = 'open5e' and table_name = ? and column_name = ?)""",
                Boolean.class, table, column));
    }
}
