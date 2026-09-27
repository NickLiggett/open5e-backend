package com.main.app.character;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record BackgroundDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<Benefit> benefits,
        String desc,
        CrossReferences crossreferences
) {

    public static BackgroundDTO from(Background entity) {
        return new BackgroundDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getBenefits(),
                entity.getDesc(),
                entity.getCrossreferences()
        );
    }
}
