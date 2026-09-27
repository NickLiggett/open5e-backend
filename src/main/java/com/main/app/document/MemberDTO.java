package com.main.app.document;

/** @param role {@code OWNER}, {@code EDITOR} or {@code VIEWER} */
public record MemberDTO(String username, String role) {
}
