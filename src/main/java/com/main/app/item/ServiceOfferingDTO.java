package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

import java.math.BigDecimal;

public record ServiceOfferingDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        BigDecimal cost,
        String detail,
        CrossReferences crossreferences
) {

    public static ServiceOfferingDTO from(ServiceOffering entity) {
        return new ServiceOfferingDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getCost(),
                entity.getDetail(),
                entity.getCrossreferences()
        );
    }
}
