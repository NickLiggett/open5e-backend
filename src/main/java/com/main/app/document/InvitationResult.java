package com.main.app.document;

/**
 * What inviting an address did.
 *
 * @param email     the address, as stored
 * @param role      the role it was given
 * @param username  set when the address already belongs to a user, who was added to the document straight away
 *                  (there's nothing to wait for, so no invitation and no email)
 * @param emailSent whether the invitation email was handed to the mail server; {@code false} when a user was added
 *                  directly, and when the mail server couldn't be reached (the invitation is kept and works anyway)
 */
public record InvitationResult(String email, String role, String username, boolean emailSent) {
}
