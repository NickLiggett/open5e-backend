package com.main.app.reference;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record SkillDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<Description> descriptions,
        String ability
) {

    public static SkillDTO from(Skill entity) {
        return new SkillDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDescriptions(),
                entity.getAbility()
        );
    }
}
