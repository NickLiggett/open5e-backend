package com.main.app.spell;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

public record SpellSchoolDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        CrossReferences crossreferences
) {

    public static SpellSchoolDTO from(SpellSchool entity) {
        return new SpellSchoolDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getCrossreferences()
        );
    }
}
