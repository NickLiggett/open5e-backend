package com.main.app.reference;

import com.main.app.common.Description;
import com.main.app.common.DocumentSummary;
import com.main.app.common.ImageReference;

import java.util.List;

public record ConditionDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        ImageReference icon,
        List<Description> descriptions
) {

    public static ConditionDTO from(Condition entity) {
        return new ConditionDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getIcon(),
                entity.getDescriptions()
        );
    }
}
