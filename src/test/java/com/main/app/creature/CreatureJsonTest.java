package com.main.app.creature;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * API (camelCase) serialization of the creature records.
 */
class CreatureJsonTest {

    private final JsonMapper apiMapper = new JsonMapper();

    @Test
    void omitsMissingSpeeds() {
        CreatureSpeed speed = new CreatureSpeed("feet", 30, null, 30, null, null, null, null);

        assertEquals("{\"unit\":\"feet\",\"walk\":30,\"swim\":30}", apiMapper.writeValueAsString(speed));
    }

    @Test
    void omitsMissingSkillsAndUsesCamelCase() {
        SkillBonuses skills = new SkillBonuses(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, 4, 6, null);

        assertEquals("{\"sleightOfHand\":4,\"stealth\":6}", apiMapper.writeValueAsString(skills));
    }
}
