package com.main.app.dtos.Creature;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A bonus per skill. For {@code skillBonuses} only the proficient skills are present, so absent ones are left out of
 * the JSON rather than sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SkillBonuses(
        Integer acrobatics,
        Integer animalHandling,
        Integer arcana,
        Integer athletics,
        Integer deception,
        Integer history,
        Integer insight,
        Integer intimidation,
        Integer investigation,
        Integer medicine,
        Integer nature,
        Integer perception,
        Integer performance,
        Integer persuasion,
        Integer religion,
        Integer sleightOfHand,
        Integer stealth,
        Integer survival
) {
}
