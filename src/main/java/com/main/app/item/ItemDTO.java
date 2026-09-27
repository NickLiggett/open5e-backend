package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.math.BigDecimal;

public record ItemDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        NamedReference category,
        WeaponStats weapon,
        ArmorStats armor,
        NamedReference size,
        BigDecimal weight,
        String weightUnit,
        BigDecimal cost,
        CrossReferences crossreferences
) {

    public static ItemDTO from(Item entity) {
        return new ItemDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getCategory(),
                entity.getWeapon(),
                entity.getArmor(),
                entity.getSize(),
                entity.getWeight(),
                entity.getWeightUnit(),
                entity.getCost(),
                entity.getCrossreferences()
        );
    }
}
