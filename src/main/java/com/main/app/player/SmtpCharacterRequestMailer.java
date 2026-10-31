package com.main.app.player;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends the request through the configured SMTP server ({@code spring.mail.*}). */
@Component
class SmtpCharacterRequestMailer implements CharacterRequestMailer {

    private final JavaMailSender sender;
    private final String from;
    private final String appUrl;

    SmtpCharacterRequestMailer(JavaMailSender sender,
                               @Value("${app.mail.from}") String from,
                               @Value("${app.public-url}") String appUrl) {
        this.sender = sender;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public void send(String email, String owner, String characterName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(owner + " asked you to play \"" + characterName + "\"");
        message.setText("""
                %s asked you to play their character "%s" in Initiative Tracker.

                Nothing happens unless you say yes. To answer, open %s and sign in, or create an account, with \
                this email address (%s), then accept or decline under Requests on the Characters page. \
                Use the address this message was sent to: the request is only for it, and you'll need to verify it \
                if you're new.

                The request expires in %d days. If you weren't expecting it, you can decline it or ignore this message.
                """.formatted(owner, characterName, appUrl, email, CharacterRequestService.VALID_DAYS));
        sender.send(message);
    }
}
