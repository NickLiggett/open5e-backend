package com.main.app.player;

/**
 * A party the current user has been asked to join, or is in.
 *
 * @param dm     the username of the party's DM
 * @param status {@code PENDING} until the current user accepts, then {@code ACCEPTED}
 */
public record PartyInvitationDTO(String dm, String status) {
}
