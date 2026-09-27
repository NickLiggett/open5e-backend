package com.main.app.spell;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.math.BigDecimal;
import java.util.List;

public record SpellDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<CastingOption> castingOptions,
        NamedReference school,
        List<NamedReference> classes,
        String rangeUnit,
        String shapeSizeUnit,
        String desc,
        Integer level,
        String higherLevel,
        String targetType,
        String rangeText,
        Integer range,
        Boolean ritual,
        String castingTime,
        String reactionCondition,
        Boolean verbal,
        Boolean somatic,
        Boolean material,
        String materialSpecified,
        BigDecimal materialCost,
        Boolean materialConsumed,
        Integer targetCount,
        String savingThrowAbility,
        Boolean attackRoll,
        String damageRoll,
        List<String> damageTypes,
        String duration,
        String shapeType,
        Integer shapeSize,
        Boolean concentration,
        CrossReferences crossreferences
) {

    public static SpellDTO from(Spell entity) {
        return new SpellDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getCastingOptions(),
                entity.getSchool(),
                entity.getClasses(),
                entity.getRangeUnit(),
                entity.getShapeSizeUnit(),
                entity.getDesc(),
                entity.getLevel(),
                entity.getHigherLevel(),
                entity.getTargetType(),
                entity.getRangeText(),
                entity.getRange(),
                entity.getRitual(),
                entity.getCastingTime(),
                entity.getReactionCondition(),
                entity.getVerbal(),
                entity.getSomatic(),
                entity.getMaterial(),
                entity.getMaterialSpecified(),
                entity.getMaterialCost(),
                entity.getMaterialConsumed(),
                entity.getTargetCount(),
                entity.getSavingThrowAbility(),
                entity.getAttackRoll(),
                entity.getDamageRoll(),
                entity.getDamageTypes(),
                entity.getDuration(),
                entity.getShapeType(),
                entity.getShapeSize(),
                entity.getConcentration(),
                entity.getCrossreferences()
        );
    }
}
