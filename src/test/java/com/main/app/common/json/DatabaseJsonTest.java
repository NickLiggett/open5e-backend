package com.main.app.common.json;

import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;
import com.main.app.creature.CreatureAction;
import com.main.app.creature.CreatureAction.ActionType;
import com.main.app.creature.CreatureAction.UsageLimits;
import com.main.app.creature.CreatureAttack;
import com.main.app.creature.CreatureAttack.AttackType;
import com.main.app.creature.SkillBonuses;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Reading database JSON (snake_case) into records with {@link DatabaseJson#MAPPER}.
 */
class DatabaseJsonTest {

    @Test
    void readsSnakeCaseKeys() {
        DocumentSummary document = DatabaseJson.MAPPER.readValue("""
                {"key": "a5e-mm", "name": "Monstrous Menagerie", "type": "SOURCE",
                 "publisher": {"key": "en-publishing", "name": "EN Publishing"},
                 "gamesystem": {"key": "a5e", "name": "Advanced 5th Edition"},
                 "display_name": "Monstrous Menagerie"}""", DocumentSummary.class);

        assertEquals(new NamedReference("a5e", "Advanced 5th Edition"), document.gamesystem());
        assertEquals("Monstrous Menagerie", document.displayName());

        SkillBonuses skills = DatabaseJson.MAPPER.readValue("{\"stealth\": 6, \"sleight_of_hand\": 4}", SkillBonuses.class);
        assertEquals(4, skills.sleightOfHand());
        assertNull(skills.arcana());
    }

    @Test
    void readsActions() {
        List<CreatureAction> actions = DatabaseJson.MAPPER.readValue("""
                [{"name": "Poison Ink Knife", "desc": "Melee Weapon Attack: +4 to hit.", "action_type": "ACTION",
                  "order_in_statblock": 0, "legendary_action_cost": null, "limited_to_form": null,
                  "usage_limits": {"type": "RECHARGE_ON_ROLL", "param": 5},
                  "crossreferences": {"to": []},
                  "attacks": [{"name": "Poison Ink Knife attack", "attack_type": "WEAPON", "to_hit_mod": 4,
                               "reach": 5, "range": null, "long_range": null, "distance_unit": "feet",
                               "target_creature_only": false, "damage_die_count": 1, "damage_die_type": "D4",
                               "damage_bonus": 2, "damage_type": null, "extra_damage_die_count": 3,
                               "extra_damage_die_type": "D6", "extra_damage_bonus": 0,
                               "extra_damage_type": {"key": "poison", "name": "Poison"}}]}]""",
                new TypeReference<>() {});

        CreatureAction action = actions.getFirst();
        assertEquals(ActionType.ACTION, action.actionType());
        assertEquals(new UsageLimits(UsageLimits.Type.RECHARGE_ON_ROLL, 5), action.usageLimits());
        CreatureAttack attack = action.attacks().getFirst();
        assertEquals(AttackType.WEAPON, attack.attackType());
        assertEquals("D4", attack.damageDieType());
        assertEquals(new NamedReference("poison", "Poison"), attack.extraDamageType());
    }

    @Test
    void readsUnknownEnumValuesAsNull() {
        CreatureAction action = DatabaseJson.MAPPER.readValue("{\"name\": \"Lair\", \"action_type\": \"LAIR_ACTION\"}",
                CreatureAction.class);

        assertNull(action.actionType());
        assertEquals("Lair", action.name());
    }
}
