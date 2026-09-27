package com.main.app.service;

import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.entity.Creature;
import com.main.app.repository.CreatureRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that every JSON column of every creature in the database survives the mapper unchanged: each DTO field is
 * written back out as snake_case JSON and compared with the original column. A dropped field, a lossy type or an
 * unrecognised enum value shows up as a mismatch. Skipped when the database has no creatures.
 */
@SpringBootTest
class CreatureMapperDataTest {

    private static final JsonMapper SNAKE_CASE = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private static final Map<String, Column> COLUMNS = Map.ofEntries(
            column("document", Creature::getDocument, CreatureDTO::document),
            column("type", Creature::getType, CreatureDTO::type),
            column("size", Creature::getSize, CreatureDTO::size),
            column("speed", Creature::getSpeed, CreatureDTO::speed),
            column("speed_all", Creature::getSpeedAll, CreatureDTO::speedAll),
            column("languages", Creature::getLanguages, CreatureDTO::languages),
            column("ability_scores", Creature::getAbilityScores, CreatureDTO::abilityScores),
            column("modifiers", Creature::getModifiers, CreatureDTO::modifiers),
            column("saving_throws", Creature::getSavingThrows, CreatureDTO::savingThrows),
            column("saving_throws_all", Creature::getSavingThrowsAll, CreatureDTO::savingThrowsAll),
            column("skill_bonuses", Creature::getSkillBonuses, CreatureDTO::skillBonuses),
            column("skill_bonuses_all", Creature::getSkillBonusesAll, CreatureDTO::skillBonusesAll),
            column("resistances_and_immunities", Creature::getResistancesAndImmunities,
                    CreatureDTO::resistancesAndImmunities),
            column("actions", Creature::getActions, CreatureDTO::actions),
            column("traits", Creature::getTraits, CreatureDTO::traits),
            column("creaturesets", Creature::getCreatureSets, CreatureDTO::creatureSets),
            column("environments", Creature::getEnvironments, CreatureDTO::environments),
            column("illustration", Creature::getIllustration, CreatureDTO::illustration),
            column("crossreferences", Creature::getCrossreferences, CreatureDTO::crossreferences)
    );

    @Autowired
    private CreatureRepository creatureRepository;

    @Autowired
    private CreatureMapper creatureMapper;

    @Test
    void everyJsonColumnRoundTrips() {
        List<Creature> creatures = creatureRepository.findAll();
        assumeTrue(!creatures.isEmpty(), "No creatures in the database");

        List<String> mismatches = new ArrayList<>();
        for (Creature creature : creatures) {
            CreatureDTO dto = creatureMapper.toDto(creature);
            COLUMNS.forEach((name, column) -> {
                JsonNode expected = withoutNulls(read(column.raw().apply(creature)));
                JsonNode actual = withoutNulls(SNAKE_CASE.valueToTree(column.parsed().apply(dto)));
                if (!expected.equals(actual)) {
                    mismatches.add(creature.getKey() + "." + name + "\n  expected: " + expected
                            + "\n  actual:   " + actual);
                }
            });
        }

        assertTrue(mismatches.isEmpty(), mismatches.size() + " mismatches across " + creatures.size()
                + " creatures, first ones:\n" + String.join("\n", mismatches.subList(0, Math.min(10, mismatches.size()))));
    }

    private static JsonNode read(String json) {
        return json == null ? SNAKE_CASE.nullNode() : SNAKE_CASE.readTree(json);
    }

    /** Explicit nulls and absent keys mean the same thing here, so both sides drop null-valued object fields. */
    private static JsonNode withoutNulls(JsonNode node) {
        if (node.isObject()) {
            ObjectNode copy = SNAKE_CASE.createObjectNode();
            node.properties().forEach(e -> {
                if (!e.getValue().isNull()) {
                    copy.set(e.getKey(), withoutNulls(e.getValue()));
                }
            });
            return copy;
        }
        if (node.isArray()) {
            var copy = SNAKE_CASE.createArrayNode();
            node.forEach(child -> copy.add(withoutNulls(child)));
            return copy;
        }
        return node;
    }

    private static Map.Entry<String, Column> column(String name, Function<Creature, String> raw,
                                                    Function<CreatureDTO, Object> parsed) {
        return Map.entry(name, new Column(raw, parsed));
    }

    private record Column(Function<Creature, String> raw, Function<CreatureDTO, Object> parsed) {
    }
}
