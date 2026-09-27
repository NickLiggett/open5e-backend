package com.main.app.item;

import com.main.app.common.DocumentSummary;

public record ItemCategoryDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom
) {

    public static ItemCategoryDTO from(ItemCategory entity) {
        return new ItemCategoryDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom()
        );
    }
}
