package com.main.app.player;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

/**
 * What the backend insists on for a character's sheet, which is otherwise the app's business: a JSON object of limited
 * size, depth and width, with no text that runs on forever. It is only kept and given back, never read.
 */
final class SheetChecks {

    /** The whole sheet as JSON, in characters. */
    static final int MAX_CHARS = 200 * 1024;
    /** How deep objects and lists may nest. */
    static final int MAX_DEPTH = 8;
    /** The most entries in one object or list. */
    static final int MAX_ENTRIES = 1000;
    /** The longest text, such as a backstory. */
    static final int MAX_TEXT = 20_000;

    private SheetChecks() {
    }

    /** The sheet itself if it is fit to keep; otherwise a 400 that says what is wrong with it. */
    static JsonNode checked(JsonNode sheet, String asJson) {
        if (sheet == null || !sheet.isObject()) {
            throw badRequest("A sheet is a JSON object");
        }
        if (asJson.length() > MAX_CHARS) {
            throw badRequest("The sheet is too big: " + MAX_CHARS / 1024 + " KB of JSON at most");
        }
        walk(sheet, 1);
        return sheet;
    }

    private static void walk(JsonNode node, int depth) {
        if (depth > MAX_DEPTH) {
            throw badRequest("The sheet nests more than " + MAX_DEPTH + " levels deep");
        }
        if (node.isString() && node.asString().length() > MAX_TEXT) {
            throw badRequest("A text on the sheet can be up to " + MAX_TEXT + " characters");
        }
        if (node.isContainer()) {
            if (node.size() > MAX_ENTRIES) {
                throw badRequest("A list or group on the sheet can have up to " + MAX_ENTRIES + " entries");
            }
            for (JsonNode child : node) {
                walk(child, depth + 1);
            }
        }
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
