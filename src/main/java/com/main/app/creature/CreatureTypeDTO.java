package com.main.app.creature;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record CreatureTypeDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<Description> descriptions
) {

    public static CreatureTypeDTO from(CreatureType entity) {
        return new CreatureTypeDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDescriptions()
        );
    }
}
