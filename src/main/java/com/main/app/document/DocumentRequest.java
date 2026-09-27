package com.main.app.document;

/**
 * Body for creating or changing a document.
 *
 * @param slug for new documents, the end of the key ({@code u{userId}-{slug}}); defaults to a slug of the name
 */
public record DocumentRequest(String name, String slug, String displayName, String desc, String author) {
}
