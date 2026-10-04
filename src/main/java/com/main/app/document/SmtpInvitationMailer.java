package com.main.app.document;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends the invitation through the configured SMTP server ({@code spring.mail.*}). */
@Component
class SmtpInvitationMailer implements InvitationMailer {

    private final JavaMailSender sender;
    private final String from;
    private final String appUrl;

    SmtpInvitationMailer(JavaMailSender sender,
                         @Value("${app.mail.from}") String from,
                         @Value("${app.public-url}") String appUrl) {
        this.sender = sender;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public void send(String email, String inviter, String documentName, String role) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(inviter + " shared \"" + documentName + "\" with you");
        message.setText("""
                %s invited you to %s the homebrew document "%s" in Initiative Tracker.

                To accept, open %s and sign in, or create an account, with this email address (%s). \
                Use the address this message was sent to: the invitation is only for it, and you'll need to \
                verify it if you're new.

                The invitation expires in %d days. If you weren't expecting it, you can ignore this message.
                """.formatted(inviter, role.equals(DocumentAccess.EDITOR) ? "edit" : "view", documentName, appUrl,
                email, InvitationService.VALID_DAYS));
        sender.send(message);
    }
}
