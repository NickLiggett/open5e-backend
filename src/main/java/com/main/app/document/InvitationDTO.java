package com.main.app.document;

import java.time.Instant;

/** A pending invitation: the document is shared with whoever signs in with this verified address before it expires. */
public record InvitationDTO(Long id, String email, String role, String invitedBy, Instant createdAt, Instant expiresAt) {
}
