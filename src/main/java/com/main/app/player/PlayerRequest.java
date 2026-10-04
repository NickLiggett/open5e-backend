package com.main.app.player;

/**
 * A player character as sent to create or replace one. Only {@code name} and {@code ruleset} are needed.
 *
 * @param ruleset         {@code 5e-2014} or {@code 5e-2024}, the keys of the game systems
 * @param classKey        the key of an Open5e class, if one was picked; {@code className} is what's shown either way
 * @param level           1 to 20, 1 if left out
 * @param initiativeBonus 0 if left out
 * @param playedBy        the username of the user who plays the character, or null
 */
public record PlayerRequest(String name, String ruleset, String classKey, String className, String speciesKey,
                            String speciesName, Integer level, Integer armorClass, Integer hitPoints,
                            Integer initiativeBonus, String notes, String playedBy) {
}
