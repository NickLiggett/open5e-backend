package com.main.app.creature;

import com.main.app.common.NamedReference;

public record CreatureAttack(
        String name,
        AttackType attackType,
        Integer toHitMod,
        Integer reach,
        Integer range,
        Integer longRange,
        String distanceUnit,
        Boolean targetCreatureOnly,
        Integer damageDieCount,
        String damageDieType,
        Integer damageBonus,
        NamedReference damageType,
        Integer extraDamageDieCount,
        String extraDamageDieType,
        Integer extraDamageBonus,
        NamedReference extraDamageType
) {

    public enum AttackType {
        WEAPON, SPELL
    }
}
