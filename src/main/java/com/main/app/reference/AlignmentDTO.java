package com.main.app.reference;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;

import java.util.List;

public record AlignmentDTO(
        String key,
        DocumentSummary document,
        String derivedFrom,
        String morality,
        String societalAttitude,
        String shortName,
        List<Description> descriptions
) {

    public static AlignmentDTO from(Alignment entity) {
        return new AlignmentDTO(
                entity.getKey(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getMorality(),
                entity.getSocietalAttitude(),
                entity.getShortName(),
                entity.getDescriptions()
        );
    }
}
