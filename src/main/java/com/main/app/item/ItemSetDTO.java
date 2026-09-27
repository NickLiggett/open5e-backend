package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record ItemSetDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<NamedReference> items,
        String desc,
        CrossReferences crossreferences
) {

    public static ItemSetDTO from(ItemSet entity) {
        return new ItemSetDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getItems(),
                entity.getDesc(),
                entity.getCrossreferences()
        );
    }
}
