package com.main.app.document;

/** Tells someone they've been invited to a document. */
public interface InvitationMailer {

    /** @throws RuntimeException if the email couldn't be sent */
    void send(String email, String inviter, String documentName, String role);
}
