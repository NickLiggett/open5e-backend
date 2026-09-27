package com.main.app.importer;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class ApiUrlsTest {

    @Test
    void rewritesOpen5eLinksOnAnyHost() {
        assertThat(ApiUrls.rewrite("https://api.open5e.com/v2/spells/srd_fireball/")).isEqualTo("/api/spells/srd_fireball");
        assertThat(ApiUrls.rewrite("http://localhost:8000/v2/classes/srd_cleric/")).isEqualTo("/api/classes/srd_cleric");
        assertThat(ApiUrls.rewrite("https://api.open5e.com/v2/items/srd_orb/?format=json")).isEqualTo("/api/items/srd_orb");
    }

    @Test
    void leavesEverythingElseAlone() {
        assertThat(ApiUrls.rewrite("https://dnd.wizards.com/resources/systems-reference-document"))
                .isEqualTo("https://dnd.wizards.com/resources/systems-reference-document");
        assertThat(ApiUrls.rewrite("/static/img/monsters/aboleth.png")).isEqualTo("/static/img/monsters/aboleth.png");
        assertThat(ApiUrls.rewrite("See https://api.open5e.com/v2/spells/srd_fireball/ for details"))
                .isEqualTo("See https://api.open5e.com/v2/spells/srd_fireball/ for details");
        assertThat(ApiUrls.rewrite("https://api.open5e.com/v2/spells/")).isEqualTo("https://api.open5e.com/v2/spells/");
    }

    @Test
    void rewritesNestedJson() {
        JsonMapper json = new JsonMapper();
        String rewritten = ApiUrls.rewrite(json.readTree("""
                {"to": [{"anchor": "Orb", "url": "https://api.open5e.com/v2/items/srd_orb/"}], "count": 1}""")).toString();

        assertThat(rewritten).isEqualTo("{\"to\":[{\"anchor\":\"Orb\",\"url\":\"/api/items/srd_orb\"}],\"count\":1}");
    }
}
