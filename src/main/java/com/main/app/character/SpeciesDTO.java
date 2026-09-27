package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record SpeciesDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        Boolean isSubspecies,
        List<SpeciesTrait> traits,
        String desc,
        String subspeciesOfKey,
        CrossReferences crossreferences
) {

    public static SpeciesDTO from(Species entity) {
        return new SpeciesDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getIsSubspecies(),
                entity.getTraits(),
                entity.getDesc(),
                entity.getSubspeciesOfKey(),
                entity.getCrossreferences()
        );
    }
}
