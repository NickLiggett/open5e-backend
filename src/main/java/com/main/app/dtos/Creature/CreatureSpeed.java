package com.main.app.dtos.Creature;

public record CreatureSpeed(
        String unit,
        Float walk,
        Float fly,
        Float swim,
        Float climb,
        Float burrow,
        Boolean hover
) {
}
