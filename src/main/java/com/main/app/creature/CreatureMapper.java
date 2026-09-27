package com.main.app.creature;

import org.springframework.stereotype.Component;

/**
 * Maps {@link Creature} entities to {@link CreatureDTO}s. The nested records are shared, so this is a field copy that
 * keeps the API shape independent of the entity.
 */
@Component
public class CreatureMapper {

    public CreatureDTO toDto(Creature creature) {
        return new CreatureDTO(
                creature.getKey(),
                creature.getName(),
                creature.getDocument().toSummary(),
                creature.getDerivedFrom(),
                creature.getType(),
                creature.getSize(),
                creature.getChallengeRating(),
                creature.getProficiencyBonus(),
                creature.getSpeed(),
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
}
