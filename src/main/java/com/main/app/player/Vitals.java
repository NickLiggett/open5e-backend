package com.main.app.player;

import java.util.List;

/**
 * What changes during play. As a request, a field left out (null) is left as it is, so the tracker can send only the
 * hit points and not undo someone else's condition.
 *
 * @param currentHp          0 up to the maximum; as a reply it is the maximum for a character never hurt, and null if the
 *                           character has no maximum
 * @param tempHp             temporary hit points, 0 to 999
 * @param heroicInspiration  whether the character has it
 * @param conditions         official condition names, such as {@code Poisoned}; exhaustion is separate
 * @param exhaustion         0 to 6
 * @param deathSaveSuccesses 0 to 3
 * @param deathSaveFailures  0 to 3
 */
public record Vitals(Integer currentHp, Integer tempHp, Boolean heroicInspiration, List<String> conditions,
                     Integer exhaustion, Integer deathSaveSuccesses, Integer deathSaveFailures) {
}
