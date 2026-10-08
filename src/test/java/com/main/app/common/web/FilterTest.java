package com.main.app.common.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * List filters, sorting and paging. Each filter's total is compared with the same condition written in SQL (all
 * rows here are default content, so no visibility condition is needed).
 */
@SpringBootTest
@AutoConfigureMockMvc
class FilterTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            /api/spells?level=3                          | select count(*) from open5e.spells where level = 3
            /api/spells?school=evocation                 | select count(*) from open5e.spells where school->>'key' = 'evocation'
            /api/spells?class=srd-2024_wizard            | select count(*) from open5e.spells where classes @> '[{"key": "srd-2024_wizard"}]'
            /api/spells?damageType=fire                  | select count(*) from open5e.spells where damage_types @> '["fire"]'
            /api/spells?concentration=true&ritual=false  | select count(*) from open5e.spells where concentration and not ritual
            /api/spells?name=FIRE                        | select count(*) from open5e.spells where lower(name) like '%fire%'
            /api/spells?name=%                           | select count(*) from open5e.spells where position('%' in name) > 0
            /api/spells?document=srd-2014,srd-2024       | select count(*) from open5e.spells where document_key in ('srd-2014', 'srd-2024')
            /api/magicitems?rarity=legendary             | select count(*) from open5e.magicitems where rarity_key = 'legendary'
            /api/magicitems?category=wand&requiresAttunement=true | select count(*) from open5e.magicitems where category_key = 'wand' and requires_attunement
            /api/items?category=armor                    | select count(*) from open5e.items where category_key = 'armor'
            /api/creatures?crMin=5&crMax=10              | select count(*) from open5e.creatures where challenge_rating between 5 and 10
            /api/creatures?cr=0.25                       | select count(*) from open5e.creatures where challenge_rating = 0.25
            /api/creatures?type=dragon&size=huge         | select count(*) from open5e.creatures where type->>'key' = 'dragon' and size->>'key' = 'huge'
            /api/classes?subclass=false                  | select count(*) from open5e.classes where subclass_of_key is null
            /api/classes?subclassOf=srd-2024_fighter     | select count(*) from open5e.classes where subclass_of_key = 'srd-2024_fighter'
            /api/species?isSubspecies=true               | select count(*) from open5e.species where is_subspecies and document_key in (select key from open5e.documents where owner_id is null)
            /api/rules?ruleset=srd-2024_combat           | select count(*) from open5e.rules where ruleset = 'srd-2024_combat'
            /api/skills?ability=dex                      | select count(*) from open5e.skills where ability = 'dex'
            /api/feats?hasPrerequisite=true              | select count(*) from open5e.feats where has_prerequisite
            /api/weapons?isSimple=true                   | select count(*) from open5e.weapons where is_simple
            /api/documents?publisher=kobold-press        | select count(*) from open5e.documents where publisher->>'key' = 'kobold-press' and owner_id is null
            /api/documents?gamesystem=5e-2024            | select count(*) from open5e.documents where gamesystem->>'key' = '5e-2024' and owner_id is null
            """)
    void filterMatchesSql(String url, String sql) throws Exception {
        int expected = jdbc.queryForObject(sql, Integer.class);
        String body = mvc.perform(get(url).param("pageSize", "1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertEquals(expected, (int) JsonPath.read(body, "$.page.totalElements"), url);
        if (!sql.contains("owner_id is null")) {
            int total = jdbc.queryForObject(sql.replaceAll(" where .*", ""), Integer.class);
            assertTrue(expected < total, "filter should narrow the results: " + url);
        }
    }

    @Test
    void subspeciesOf() throws Exception {
        String parent = jdbc.queryForObject(
                "select subspecies_of_key from open5e.species where subspecies_of_key is not null limit 1", String.class);
        int expected = jdbc.queryForObject("select count(*) from open5e.species where subspecies_of_key = ?",
                Integer.class, parent);

        mvc.perform(get("/api/species").param("subspeciesOf", parent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(expected))
                .andExpect(jsonPath("$.content[0].subspeciesOfKey").value(parent));
    }

    @Test
    void sortsByRequestedProperty() throws Exception {
        String last = jdbc.queryForObject("select max(key) from open5e.spells", String.class);

        mvc.perform(get("/api/spells").param("sort", "key,desc").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].key").value(last));
    }

    @Test
    void sortsByKeyByDefault() throws Exception {
        String first = jdbc.queryForObject("select min(key) from open5e.creatures", String.class);

        mvc.perform(get("/api/creatures").param("pageSize", "1"))
                .andExpect(jsonPath("$.content[0].key").value(first));
    }

    @Test
    void capsPageSize() throws Exception {
        mvc.perform(get("/api/spells").param("pageSize", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(500));
    }

    @Test
    void rejectsUnknownSortProperty() throws Exception {
        mvc.perform(get("/api/spells").param("sort", "nope"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Can't sort by 'nope'"));
    }

    @Test
    void rejectsFilterValuesOfTheWrongType() throws Exception {
        mvc.perform(get("/api/spells").param("level", "three"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Invalid value 'three' for 'level'"));
    }
}
