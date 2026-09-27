package com.main.app.item;

import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record WeaponDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        List<WeaponPropertyUse> properties,
        NamedReference damageType,
        String distanceUnit,
        String damageDice,
        Integer range,
        Integer longRange,
        Boolean isSimple,
        Boolean isImprovised
) {

    public static WeaponDTO from(Weapon entity) {
        return new WeaponDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getProperties(),
                entity.getDamageType(),
                entity.getDistanceUnit(),
                entity.getDamageDice(),
                entity.getRange(),
                entity.getLongRange(),
                entity.getIsSimple(),
                entity.getIsImprovised()
        );
    }
}
