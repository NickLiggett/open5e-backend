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
        String derivedFrom,
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

    public static CreatureDTO from(Creature entity) {
        return new CreatureDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getType(),
                entity.getSize(),
                entity.getChallengeRating(),
                entity.getProficiencyBonus(),
                entity.getSpeed(),
                entity.getSpeedAll(),
                entity.getCategory(),
                entity.getSubcategory(),
                entity.getAlignment(),
                entity.getLanguages(),
                entity.getArmorClass(),
                entity.getArmorDetail(),
                entity.getHitPoints(),
                entity.getHitDice(),
                entity.getExperiencePoints(),
                entity.getAbilityScores(),
                entity.getModifiers(),
                entity.getInitiativeBonus(),
                entity.getSavingThrows(),
                entity.getSavingThrowsAll(),
                entity.getSkillBonuses(),
                entity.getSkillBonusesAll(),
                entity.getPassivePerception(),
                entity.getResistancesAndImmunities(),
                entity.getNormalSightRange(),
                entity.getDarkvisionRange(),
                entity.getBlindsightRange(),
                entity.getTremorsenseRange(),
                entity.getTruesightRange(),
                entity.getActions(),
                entity.getTraits(),
                entity.getCreatureSets(),
                entity.getEnvironments(),
                entity.getIllustration(),
                entity.getCrossreferences()
        );
    }
}
