package com.main.app.ownership;

import java.text.Normalizer;
import java.util.Locale;

/** Keys for user-created content. Open5e keys look like {@code {document}_{slug}}; user content follows suit. */
public final class Keys {

    private static final int MAX_SLUG_LENGTH = 64;

    private Keys() {
    }

    /** "Owlbear King (Elite)" → {@code owlbear-king-elite}. Empty if the text has no letters or digits. */
    public static String slugify(String text) {
        if (text == null) {
            return "";
        }
        String slug = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return slug;
    }

    /** The key of a user's document: {@code u{userId}-{slug}}. */
    public static String userDocument(long userId, String slug) {
        return "u" + userId + "-" + slug;
    }

    /** The key of a resource in a document: {@code {documentKey}_{slug}}. */
    public static String resource(String documentKey, String slug) {
        return documentKey + "_" + slug;
    }

    /** The part of a resource key after its document, e.g. {@code aboleth} for {@code a5e-mm_aboleth}. */
    public static String slugOf(String resourceKey) {
        int separator = resourceKey.indexOf('_');
        return separator < 0 ? slugify(resourceKey) : resourceKey.substring(separator + 1);
    }
}
