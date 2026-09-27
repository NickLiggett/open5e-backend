package com.main.app.reference;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record AbilityDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<Description> descriptions,
        List<NamedReference> skills,
        String shortDesc
) {

    public static AbilityDTO from(Ability entity) {
        return new AbilityDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDescriptions(),
                entity.getSkills(),
                entity.getShortDesc()
        );
    }
}
