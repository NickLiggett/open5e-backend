package com.main.app.item;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.math.BigDecimal;

public record MagicItemDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        NamedReference category,
        RarityReference rarity,
        WeaponStats weapon,
        ArmorStats armor,
        NamedReference size,
        BigDecimal weight,
        String weightUnit,
        BigDecimal cost,
        Boolean requiresAttunement,
        String attunementDetail,
        CrossReferences crossreferences
) {

    public static MagicItemDTO from(MagicItem entity) {
        return new MagicItemDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getCategory(),
                entity.getRarity(),
                entity.getWeapon(),
                entity.getArmor(),
                entity.getSize(),
                entity.getWeight(),
                entity.getWeightUnit(),
                entity.getCost(),
                entity.getRequiresAttunement(),
                entity.getAttunementDetail(),
                entity.getCrossreferences()
        );
    }
}
