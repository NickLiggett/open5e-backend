package com.main.app.document;

/** @param role {@code VIEWER} (can see the document's content) or {@code EDITOR} (can also change it) */
public record MemberRequest(String role) {
}
