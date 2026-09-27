package com.main.app.creature;

import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record CreatureSetDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<NamedReference> creatures
) {

    public static CreatureSetDTO from(CreatureSet entity) {
        return new CreatureSetDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getCreatures()
        );
    }
}
