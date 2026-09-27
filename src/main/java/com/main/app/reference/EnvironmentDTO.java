package com.main.app.reference;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

public record EnvironmentDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        Boolean aquatic,
        Boolean planar,
        Boolean interior,
        CrossReferences crossreferences
) {

    public static EnvironmentDTO from(Environment entity) {
        return new EnvironmentDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getAquatic(),
                entity.getPlanar(),
                entity.getInterior(),
                entity.getCrossreferences()
        );
    }
}
