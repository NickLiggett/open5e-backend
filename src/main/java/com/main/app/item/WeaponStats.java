package com.main.app.item;

import com.main.app.common.NamedReference;

import java.util.List;

/** The weapon profile of an item or magic item, e.g. a +1 longsword's longsword stats. */
public record WeaponStats(
        String key,
        String name,
        String damageDice,
        NamedReference damageType,
        String distanceUnit,
        Boolean isSimple,
        Boolean isMartial,
        Boolean isImprovised,
        List<WeaponPropertyUse> properties
) {
}
