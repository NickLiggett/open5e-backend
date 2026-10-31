package com.main.app.player;

import java.time.Instant;

/**
 * A request, as its owner sees it.
 *
 * @param status      {@code PENDING} until answered; {@code DENIED} if it was turned down (kept until the owner dismisses
 *                    it, so they know, and so the same address can't be asked again at once)
 * @param emailSent   whether the email went out; only on the response to making a request
 * @param askAgainAt  when a denied request's address may be asked again; null unless denied
 */
public record CharacterRequestDTO(long id, long playerId, String characterName, String email, String status,
                                  Instant createdAt, Instant expiresAt, Instant respondedAt, Instant askAgainAt,
                                  Boolean emailSent) {
}
