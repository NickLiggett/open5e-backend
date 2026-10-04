package com.main.app.player;

import java.time.Instant;

/**
 * A player character.
 *
 * @param owner    the username of the user who made it and manages it
 * @param playedBy the username of the user who plays it, or null
 * @param role     the current user's part in it: {@code OWNER}, {@code PLAYER} if they only play it, or
 *                 {@code PARTY} if they can only see it because its owner or player is in their party
 */
public record PlayerDTO(Long id, String name, String ruleset, String classKey, String className, String speciesKey,
                        String speciesName, int level, Integer armorClass, Integer hitPoints, int initiativeBonus,
                        String notes, String owner, String playedBy, String role, Instant createdAt,
                        Instant updatedAt) {
}
