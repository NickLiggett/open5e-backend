package com.main.app.service;

import com.main.app.dtos.Creature.AbilityScores;
import com.main.app.dtos.Creature.CreatureAction;
import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.dtos.Creature.CreatureDocument;
import com.main.app.dtos.Creature.CreatureIllustration;
import com.main.app.dtos.Creature.CreatureLanguages;
import com.main.app.dtos.Creature.CreatureSize;
import com.main.app.dtos.Creature.CreatureSpeed;
import com.main.app.dtos.Creature.CreatureTrait;
import com.main.app.dtos.Creature.CreatureType;
import com.main.app.dtos.Creature.CrossReferences;
import com.main.app.dtos.Creature.NamedReference;
import com.main.app.dtos.Creature.ResistancesAndImmunities;
import com.main.app.dtos.Creature.SkillBonuses;
import com.main.app.entity.Creature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.function.Function;

/**
 * Maps {@link Creature} entities to {@link CreatureDTO}s.
 * <p>
 * The JSON-shaped columns are stored as text in the Open5e snake_case format and are parsed into their DTOs here.
 * Document, type and size fall back to a key-only reference if the column holds a plain key instead of JSON.
 */
@Component
public class CreatureMapper {

    private static final Logger log = LoggerFactory.getLogger(CreatureMapper.class);

    private static final TypeReference<List<CreatureAction>> ACTIONS = new TypeReference<>() {};
    private static final TypeReference<List<CreatureTrait>> TRAITS = new TypeReference<>() {};
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
    private static final TypeReference<List<NamedReference>> REFERENCES = new TypeReference<>() {};

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .build();

    public CreatureDTO toDto(Creature creature) {
        String key = creature.getKey();
        return new CreatureDTO(
                key,
                creature.getName(),
                parse(key, creature.getDocument(), CreatureDocument.class,
                        docKey -> new CreatureDocument(docKey, null, null, null, null, null, null)),
                parse(key, creature.getType(), CreatureType.class, typeKey -> new CreatureType(typeKey, null)),
                parse(key, creature.getSize(), CreatureSize.class, sizeKey -> new CreatureSize(sizeKey, null)),
                creature.getChallengeRating(),
                creature.getProficiencyBonus(),
                parse(key, creature.getSpeed(), CreatureSpeed.class),
                parse(key, creature.getSpeedAll(), CreatureSpeed.class),
                creature.getCategory(),
                creature.getSubcategory(),
                creature.getAlignment(),
                parse(key, creature.getLanguages(), CreatureLanguages.class),
                creature.getArmorClass(),
                creature.getArmorDetail(),
                creature.getHitPoints(),
                creature.getHitDice(),
                creature.getExperiencePoints(),
                parse(key, creature.getAbilityScores(), AbilityScores.class),
                parse(key, creature.getModifiers(), AbilityScores.class),
                creature.getInitiativeBonus(),
                parse(key, creature.getSavingThrows(), AbilityScores.class),
                parse(key, creature.getSavingThrowsAll(), AbilityScores.class),
                parse(key, creature.getSkillBonuses(), SkillBonuses.class),
                parse(key, creature.getSkillBonusesAll(), SkillBonuses.class),
                creature.getPassivePerception(),
                parse(key, creature.getResistancesAndImmunities(), ResistancesAndImmunities.class),
                creature.getNormalSightRange(),
                creature.getDarkvisionRange(),
                creature.getBlindsightRange(),
                creature.getTremorsenseRange(),
                creature.getTruesightRange(),
                parse(key, creature.getActions(), ACTIONS),
                parse(key, creature.getTraits(), TRAITS),
                parse(key, creature.getCreatureSets(), STRINGS),
                parse(key, creature.getEnvironments(), REFERENCES),
                parse(key, creature.getIllustration(), CreatureIllustration.class),
                parse(key, creature.getCrossreferences(), CrossReferences.class)
        );
    }

    private <T> T parse(String creatureKey, String value, Class<T> type, Function<String, T> fromKey) {
        if (value != null && !value.isBlank() && !value.trim().startsWith("{")) {
            return fromKey.apply(value.trim());
        }
        return parse(creatureKey, value, type);
    }

    private <T> T parse(String creatureKey, String value, Class<T> type) {
        return parse(creatureKey, value, jsonMapper.constructType(type));
    }

    private <T> T parse(String creatureKey, String value, TypeReference<T> type) {
        return parse(creatureKey, value, jsonMapper.constructType(type));
    }

    private <T> T parse(String creatureKey, String value, JavaType type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(value, type);
        } catch (JacksonException e) {
            log.warn("Could not parse {} for creature {}", type.toCanonical(), creatureKey, e);
            return null;
        }
    }
}
