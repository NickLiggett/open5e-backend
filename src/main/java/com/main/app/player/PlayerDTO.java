package com.main.app.player;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

/**
 * A player character.
 *
 * @param classKey  the first class's key, for anything that knows only one class
 * @param className the first class's name
 * @param level     the character's level: the total of the levels in {@code classes} if it has any
 * @param owner     the username of the user who made it and manages it
 * @param playedBy  the username of the user who plays it, or null; it is the owner's own when they play it themselves
 * @param role      the current user's part in it: {@code OWNER}, {@code PLAYER} if they only play it, or
 *                  {@code PARTY} if they can only see it because its owner or player is in their party
 * @param classes   every class with its levels, first class first; empty if no class was given
 * @param vitals    what changes during play: current and temporary hit points, inspiration, conditions, death saves
 * @param sheet     the rest of the sheet, a JSON object the app shapes; empty until filled in
 * @param version   counts the saves of the rest of the character; send it back to have a stale save refused
 */
public record PlayerDTO(Long id, String name, String ruleset, String classKey, String className, String speciesKey,
                        String speciesName, int level, Integer armorClass, Integer hitPoints, int initiativeBonus,
                        String notes, String owner, String playedBy, String role, Instant createdAt,
                        Instant updatedAt, List<ClassLevel> classes, Vitals vitals, JsonNode sheet,
                        int version) {
}
