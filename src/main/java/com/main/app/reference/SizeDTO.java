package com.main.app.reference;

import com.main.app.common.DocumentSummary;

public record SizeDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String distanceUnit,
        Integer rank,
        Integer spaceDiameter,
        String suggestedHitDice
) {

    public static SizeDTO from(Size entity) {
        return new SizeDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDistanceUnit(),
                entity.getRank(),
                entity.getSpaceDiameter(),
                entity.getSuggestedHitDice()
        );
    }
}
