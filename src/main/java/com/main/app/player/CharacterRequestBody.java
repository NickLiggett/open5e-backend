package com.main.app.player;

/** Who to ask: an email address. It can belong to someone who hasn't signed up yet. */
public record CharacterRequestBody(String email) {
}
