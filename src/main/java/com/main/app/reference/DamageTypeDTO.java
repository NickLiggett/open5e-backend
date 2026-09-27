package com.main.app.reference;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record DamageTypeDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<Description> descriptions
) {

    public static DamageTypeDTO from(DamageType entity) {
        return new DamageTypeDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDescriptions()
        );
    }
}
