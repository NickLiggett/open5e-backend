package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record FeatDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        Boolean hasPrerequisite,
        List<Benefit> benefits,
        String desc,
        String prerequisite,
        String type,
        CrossReferences crossreferences
) {

    public static FeatDTO from(Feat entity) {
        return new FeatDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getHasPrerequisite(),
                entity.getBenefits(),
                entity.getDesc(),
                entity.getPrerequisite(),
                entity.getType(),
                entity.getCrossreferences()
        );
    }
}
