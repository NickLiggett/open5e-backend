package com.main.app.service;

import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.dtos.Creature.CreatureDocument;
import com.main.app.dtos.Creature.CreatureSpeed;
import com.main.app.dtos.Creature.NamedReference;
import com.main.app.entity.Creature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreatureMapperTest {

    private final CreatureMapper mapper = new CreatureMapper();

    @Test
    void parsesJsonColumns() {
        Creature creature = new Creature("a5e-mm_goblin");
        creature.setName("Goblin");
        creature.setDocument("""
                {"key": "a5e-mm", "name": "Monstrous Menagerie", "type": "SOURCE",
                 "publisher": {"key": "en-publishing", "name": "EN Publishing"},
                 "gamesystem": {"key": "a5e", "name": "Advanced 5th Edition"},
                 "display_name": "Monstrous Menagerie"}""");
        creature.setType("{\"key\": \"humanoid\", \"name\": \"Humanoid\"}");
        creature.setSpeed("{\"walk\": 30.0, \"unit\": \"feet\"}");

        CreatureDTO dto = mapper.toDto(creature);

        assertEquals("Goblin", dto.name());
        assertEquals(new NamedReference("a5e", "Advanced 5th Edition"), dto.document().gamesystem());
        assertEquals("Monstrous Menagerie", dto.document().displayName());
        assertEquals("Humanoid", dto.type().name());
        assertEquals(new CreatureSpeed("feet", 30.0f, null, null, null, null, null), dto.speed());
        assertNull(dto.size());
    }

    @Test
    void treatsPlainValuesAsKeys() {
        Creature creature = new Creature("a5e-mm_goblin");
        creature.setDocument("a5e-mm");
        creature.setSize("small");

        CreatureDTO dto = mapper.toDto(creature);

        assertEquals(new CreatureDocument("a5e-mm", null, null, null, null, null, null), dto.document());
        assertEquals("small", dto.size().key());
    }
}
