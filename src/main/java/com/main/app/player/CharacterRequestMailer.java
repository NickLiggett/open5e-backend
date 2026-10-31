package com.main.app.player;

/** Tells someone they've been asked to play a character. */
public interface CharacterRequestMailer {

    /** @throws RuntimeException if the email couldn't be sent */
    void send(String email, String owner, String characterName);
}
