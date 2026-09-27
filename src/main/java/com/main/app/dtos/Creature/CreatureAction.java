package com.main.app.dtos.Creature;

import java.util.List;

public record CreatureAction(
        String name,
        String desc,
        ActionType actionType,
        Integer orderInStatblock,
        Integer legendaryActionCost,
        UsageLimits usageLimits,
        String limitedToForm,
        List<CreatureAttack> attacks,
        CrossReferences crossreferences
) {

    public enum ActionType {
        ACTION, BONUS_ACTION, REACTION, LEGENDARY_ACTION
    }

    /**
     * How often an action can be used, e.g. {@code PER_DAY} with param 3, or {@code RECHARGE_ON_ROLL} with param 5
     * for "Recharge 5-6".
     */
    public record UsageLimits(Type type, Integer param) {

        public enum Type {
            PER_DAY, RECHARGE, RECHARGE_ON_ROLL
        }
    }
}
