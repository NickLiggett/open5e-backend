package com.main.app.creature;

import com.main.app.common.json.JsonColumnRoundTrip;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks every JSON column of every creature in the database against the API objects, end to end through Hibernate's
 * JSON mapping and {@link CreatureMapper}. Skipped when the database has no creatures.
 */
@SpringBootTest
class CreatureDataTest {

    private static final Map<String, Function<CreatureDTO, Object>> COLUMNS = new LinkedHashMap<>();

    static {
        COLUMNS.put("document", CreatureDTO::document);
        COLUMNS.put("type", CreatureDTO::type);
        COLUMNS.put("size", CreatureDTO::size);
        COLUMNS.put("speed", CreatureDTO::speed);
        COLUMNS.put("speed_all", CreatureDTO::speedAll);
        COLUMNS.put("languages", CreatureDTO::languages);
        COLUMNS.put("ability_scores", CreatureDTO::abilityScores);
        COLUMNS.put("modifiers", CreatureDTO::modifiers);
        COLUMNS.put("saving_throws", CreatureDTO::savingThrows);
        COLUMNS.put("saving_throws_all", CreatureDTO::savingThrowsAll);
        COLUMNS.put("skill_bonuses", CreatureDTO::skillBonuses);
        COLUMNS.put("skill_bonuses_all", CreatureDTO::skillBonusesAll);
        COLUMNS.put("resistances_and_immunities", CreatureDTO::resistancesAndImmunities);
        COLUMNS.put("actions", CreatureDTO::actions);
        COLUMNS.put("traits", CreatureDTO::traits);
        COLUMNS.put("creaturesets", CreatureDTO::creatureSets);
        COLUMNS.put("environments", CreatureDTO::environments);
        COLUMNS.put("illustration", CreatureDTO::illustration);
        COLUMNS.put("crossreferences", CreatureDTO::crossreferences);
    }

    @Autowired
    private CreatureService creatureService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyJsonColumnRoundTrips() {
        Map<String, CreatureDTO> creatures = creatureService.getAllCreatures().stream()
                .collect(Collectors.toMap(CreatureDTO::key, Function.identity()));
        assumeTrue(!creatures.isEmpty(), "No creatures in the database");

        JsonColumnRoundTrip.assertRoundTrips(jdbc, "creatures", creatures, COLUMNS);
    }
}
