package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

public record WeaponPropertyDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        String type,
        CrossReferences crossreferences
) {

    public static WeaponPropertyDTO from(WeaponProperty entity) {
        return new WeaponPropertyDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getType(),
                entity.getCrossreferences()
        );
    }
}
