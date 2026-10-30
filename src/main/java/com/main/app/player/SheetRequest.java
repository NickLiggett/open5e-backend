package com.main.app.player;

import tools.jackson.databind.JsonNode;

/**
 * The sheet of a character as sent to replace it.
 *
 * @param version the {@code version} of the character this was edited from; if it is no longer the current one, someone
 *                saved in between and the save is refused (409). Left out, nothing is checked.
 * @param sheet   a JSON object; what is in it is up to the app, within the limits of {@link SheetChecks}
 */
public record SheetRequest(Integer version, JsonNode sheet) {
}
