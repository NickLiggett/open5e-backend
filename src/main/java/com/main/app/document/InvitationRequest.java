package com.main.app.document;

/**
 * @param email the address to invite
 * @param role  {@code VIEWER} (can see the document's content) or {@code EDITOR} (can also change it)
 */
public record InvitationRequest(String email, String role) {
}
