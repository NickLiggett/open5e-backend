package com.main.app.service;

import com.main.app.dtos.Creature.CreatureAction;
import com.main.app.dtos.Creature.CreatureAction.ActionType;
import com.main.app.dtos.Creature.CreatureAction.UsageLimits;
import com.main.app.dtos.Creature.CreatureAttack;
import com.main.app.dtos.Creature.CreatureAttack.AttackType;
import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.dtos.Creature.CreatureDocument;
import com.main.app.dtos.Creature.CreatureSpeed;
import com.main.app.dtos.Creature.NamedReference;
import com.main.app.entity.Creature;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

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
        creature.setSpeed("{\"walk\": 30, \"unit\": \"feet\"}");
        creature.setSkillBonuses("{\"stealth\": 6, \"sleight_of_hand\": 4}");

        CreatureDTO dto = mapper.toDto(creature);

        assertEquals("Goblin", dto.name());
        assertEquals(new NamedReference("a5e", "Advanced 5th Edition"), dto.document().gamesystem());
        assertEquals("Monstrous Menagerie", dto.document().displayName());
        assertEquals("Humanoid", dto.type().name());
        assertEquals(new CreatureSpeed("feet", 30, null, null, null, null, null, null), dto.speed());
        assertEquals(4, dto.skillBonuses().sleightOfHand());
        assertNull(dto.skillBonuses().arcana());
        assertNull(dto.size());
    }

    @Test
    void parsesActions() {
        Creature creature = new Creature("a5e-mm_aboleth-thrall");
        creature.setActions("""
                [{"name": "Poison Ink Knife", "desc": "Melee Weapon Attack: +4 to hit.", "action_type": "ACTION",
                  "order_in_statblock": 0, "legendary_action_cost": null, "limited_to_form": null,
                  "usage_limits": {"type": "RECHARGE_ON_ROLL", "param": 5},
                  "crossreferences": {"to": []},
                  "attacks": [{"name": "Poison Ink Knife attack", "attack_type": "WEAPON", "to_hit_mod": 4,
                               "reach": 5, "range": null, "long_range": null, "distance_unit": "feet",
                               "target_creature_only": false, "damage_die_count": 1, "damage_die_type": "D4",
                               "damage_bonus": 2, "damage_type": null, "extra_damage_die_count": 3,
                               "extra_damage_die_type": "D6", "extra_damage_bonus": 0,
                               "extra_damage_type": {"key": "poison", "name": "Poison"}}]}]""");

        CreatureAction action = mapper.toDto(creature).actions().getFirst();

        assertEquals(ActionType.ACTION, action.actionType());
        assertEquals(new UsageLimits(UsageLimits.Type.RECHARGE_ON_ROLL, 5), action.usageLimits());
        CreatureAttack attack = action.attacks().getFirst();
        assertEquals(AttackType.WEAPON, attack.attackType());
        assertEquals("D4", attack.damageDieType());
        assertEquals(new NamedReference("poison", "Poison"), attack.extraDamageType());
    }

    @Test
    void omitsMissingSpeedsFromJson() {
        CreatureSpeed speed = new CreatureSpeed("feet", 30, null, 30, null, null, null, null);

        assertEquals("{\"unit\":\"feet\",\"walk\":30,\"swim\":30}", new JsonMapper().writeValueAsString(speed));
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

    @Test
    void returnsNullForMalformedJson() {
        Creature creature = new Creature("broken");
        creature.setActions("[{\"name\": ");

        assertNull(mapper.toDto(creature).actions());
    }
}
