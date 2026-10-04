package com.main.app.user;

import java.util.Locale;
import java.util.regex.Pattern;

/** Email addresses as the app compares them: trimmed and lowercased. */
public final class Emails {

    private static final int MAX_LENGTH = 254;
    private static final String PART = "[^@\\s,;<>()\\[\\]\"]+";
    private static final Pattern SHAPE = Pattern.compile(PART + "@" + PART + "\\." + PART);

    private Emails() {
    }

    /** The address, or null if it doesn't look like an email address (one {@code @}, a dot in the domain). */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.length() <= MAX_LENGTH && SHAPE.matcher(trimmed).matches() ? trimmed : null;
    }
}
