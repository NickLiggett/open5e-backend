package com.main.app.player;

/**
 * A user in the current user's party.
 *
 * @param status {@code PENDING} until they accept, then {@code ACCEPTED}
 */
public record PartyMemberDTO(String username, String status) {
}
