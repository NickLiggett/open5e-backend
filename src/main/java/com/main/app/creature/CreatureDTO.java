package com.main.app.creature;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.ImageReference;
import com.main.app.common.NamedReference;

import java.util.List;

public record CreatureDTO(
        String key,
        String name,
        DocumentSummary document,
        NamedReference type,
        NamedReference size,
        Float challengeRating,
        Integer proficiencyBonus,
        CreatureSpeed speed,
        CreatureSpeed speedAll,
        String category,
        String subcategory,
        String alignment,
        CreatureLanguages languages,
        Integer armorClass,
        String armorDetail,
        Integer hitPoints,
        String hitDice,
        Integer experiencePoints,
        AbilityScores abilityScores,
        AbilityScores modifiers,
        Integer initiativeBonus,
        AbilityScores savingThrows,
        AbilityScores savingThrowsAll,
        SkillBonuses skillBonuses,
        SkillBonuses skillBonusesAll,
        Integer passivePerception,
        ResistancesAndImmunities resistancesAndImmunities,
        Integer normalSightRange,
        Integer darkvisionRange,
        Integer blindsightRange,
        Integer tremorsenseRange,
        Integer truesightRange,
        List<CreatureAction> actions,
        List<CreatureTrait> traits,
        List<String> creatureSets,
        List<NamedReference> environments,
        ImageReference illustration,
        CrossReferences crossreferences
) {
}
