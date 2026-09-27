package com.main.app.ownership;

/**
 * Body of {@code POST /api/<resource>/{key}/copy}.
 *
 * @param document the document to copy into; if omitted, the current user's personal homebrew document
 */
public record CopyRequest(String document) {
}
