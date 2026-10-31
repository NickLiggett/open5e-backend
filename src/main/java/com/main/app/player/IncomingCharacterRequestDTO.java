package com.main.app.player;

import java.time.Instant;

/**
 * A request to play a character, as the person asked sees it. They see only what they need to decide: who asks, and the
 * character's name, class, species and level.
 *
 * @param owner the username of the user who made the character and asks
 */
public record IncomingCharacterRequestDTO(long id, String characterName, String className, String speciesName,
                                          int level, String owner, Instant createdAt, Instant expiresAt) {
}
