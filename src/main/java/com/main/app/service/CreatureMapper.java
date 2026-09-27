package com.main.app.service;

import com.main.app.dtos.Creature.CreatureDTO;
import com.main.app.dtos.Creature.CreatureDocument;
import com.main.app.dtos.Creature.CreatureSize;
import com.main.app.dtos.Creature.CreatureSpeed;
import com.main.app.dtos.Creature.CreatureType;
import com.main.app.entity.Creature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Function;

/**
 * Maps {@link Creature} entities to {@link CreatureDTO}s.
 * <p>
 * The nested columns (document, type, size, speed) are stored as text. If a column holds a JSON object in the
 * Open5e snake_case format it is parsed into its DTO; otherwise the raw value is treated as a key.
 */
@Component
public class CreatureMapper {

    private static final Logger log = LoggerFactory.getLogger(CreatureMapper.class);

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public CreatureDTO toDto(Creature creature) {
        return new CreatureDTO(
                creature.getKey(),
                creature.getName(),
                parse(creature.getDocument(), CreatureDocument.class,
                        key -> new CreatureDocument(key, null, null, null, null, null, null)),
                parse(creature.getType(), CreatureType.class, key -> new CreatureType(key, null)),
                parse(creature.getSize(), CreatureSize.class, key -> new CreatureSize(key, null)),
                creature.getChallengeRating(),
                creature.getProficiencyBonus(),
                parse(creature.getSpeed(), CreatureSpeed.class, key -> null),
                creature.getSpeedAll(),
                creature.getCategory(),
                creature.getSubcategory(),
                creature.getAlignment(),
                creature.getLanguages(),
                creature.getArmorClass(),
                creature.getArmorDetail(),
                creature.getHitPoints(),
                creature.getHitDice(),
                creature.getExperiencePoints(),
                creature.getAbilityScores(),
                creature.getModifiers(),
                creature.getInitiativeBonus(),
                creature.getSavingThrows(),
                creature.getSavingThrowsAll(),
                creature.getSkillBonuses(),
                creature.getSkillBonusesAll(),
                creature.getPassivePerception(),
                creature.getResistancesAndImmunities(),
                creature.getNormalSightRange(),
                creature.getDarkvisionRange(),
                creature.getBlindsightRange(),
                creature.getTremorsenseRange(),
                creature.getTruesightRange(),
                creature.getActions(),
                creature.getTraits(),
                creature.getCreatureSets(),
                creature.getEnvironments(),
                creature.getIllustration(),
                creature.getCrossreferences()
        );
    }

    private <T> T parse(String value, Class<T> type, Function<String, T> fromKey) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (!trimmed.startsWith("{")) {
            return fromKey.apply(trimmed);
        }
        try {
            return jsonMapper.readValue(trimmed, type);
        } catch (JacksonException e) {
            log.warn("Could not parse {} from '{}'", type.getSimpleName(), trimmed, e);
            return null;
        }
    }
}
