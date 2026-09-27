package com.main.app.character;

/** A base class's hit points (subclasses have none). */
public record HitPoints(
        String hitDice,
        String hitDiceName,
        String hitPointsAt1stLevel,
        String hitPointsAtHigherLevels
) {
}
