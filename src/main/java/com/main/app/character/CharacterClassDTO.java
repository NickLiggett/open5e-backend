package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record CharacterClassDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<ClassFeature> features,
        List<NamedReference> savingThrows,
        NamedReference subclassOf,
        String desc,
        String hitDice,
        String casterType,
        List<NamedReference> primaryAbilities,
        CrossReferences crossreferences,
        HitPoints hitPoints
) {

    public static CharacterClassDTO from(CharacterClass entity) {
        return new CharacterClassDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getFeatures(),
                entity.getSavingThrows(),
                entity.getSubclassOf(),
                entity.getDesc(),
                entity.getHitDice(),
                entity.getCasterType(),
                entity.getPrimaryAbilities(),
                entity.getCrossreferences(),
                entity.getHitPoints()
        );
    }
}
