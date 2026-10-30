package com.main.app.player;

import java.util.List;

/**
 * A player character as sent to create or replace one. Only {@code name} and {@code ruleset} are needed.
 *
 * @param ruleset         {@code 5e-2014} or {@code 5e-2024}, the keys of the game systems
 * @param classKey        the key of an Open5e class, if one was picked; {@code className} is what's shown either way
 * @param level           1 to 20, 1 if left out; with {@code classes} it is ignored, and the total of their levels is used
 * @param initiativeBonus 0 if left out
 * @param playedBy        the username of the user who plays the character, or null; it may be the owner's own
 * @param classes         the classes of a multiclass character, first class first, with a level each. If left out,
 *                        {@code classKey}, {@code className} and {@code level} make a single class.
 */
public record PlayerRequest(String name, String ruleset, String classKey, String className, String speciesKey,
                            String speciesName, Integer level, Integer armorClass, Integer hitPoints,
                            Integer initiativeBonus, String notes, String playedBy, List<ClassLevel> classes) {
}
