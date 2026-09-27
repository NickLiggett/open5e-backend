package com.main.app.item;

import com.main.app.common.DocumentSummary;

public record ArmorDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String acDisplay,
        String category,
        Boolean grantsStealthDisadvantage,
        Integer strengthScoreRequired,
        Integer acBase,
        Boolean acAddDexmod,
        Integer acCapDexmod
) {

    public static ArmorDTO from(Armor entity) {
        return new ArmorDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getAcDisplay(),
                entity.getCategory(),
                entity.getGrantsStealthDisadvantage(),
                entity.getStrengthScoreRequired(),
                entity.getAcBase(),
                entity.getAcAddDexmod(),
                entity.getAcCapDexmod()
        );
    }
}
