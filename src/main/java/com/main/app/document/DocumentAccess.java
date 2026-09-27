package com.main.app.document;

import com.main.app.common.web.ResourceNotFoundException;
import com.main.app.ownership.Keys;
import com.main.app.user.CurrentUser;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;
import java.util.Optional;

/**
 * Who may change what. Reading is governed by the visibility filter; this decides writes:
 * <ul>
 *     <li>default content (no owner): nobody</li>
 *     <li>a user's document: its owner, and members with the {@code EDITOR} role</li>
 *     <li>the document itself and its members: only the owner</li>
 * </ul>
 * Documents the current user can't see are reported as not found, never as forbidden.
 */
@Component
public class DocumentAccess {

    public static final String EDITOR = "EDITOR";
    public static final String VIEWER = "VIEWER";

    private final CurrentUser currentUser;
    private final EntityManager em;
    private final JdbcTemplate jdbc;

    public DocumentAccess(CurrentUser currentUser, EntityManager em, JdbcTemplate jdbc) {
        this.currentUser = currentUser;
        this.em = em;
        this.jdbc = jdbc;
    }

    /** @throws ResponseStatusException 401 if nobody is signed in */
    public long requireUser() {
        return currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to change content"));
    }

    /** @throws ResourceNotFoundException if the current user can't see the document */
    public Document visible(String key) {
        Document document = key == null ? null : em.find(Document.class, key);
        if (document == null) {
            throw new ResourceNotFoundException("document", key);
        }
        return document;
    }

    /** A document the current user may add content to or change content in. */
    public Document writable(String key) {
        long user = requireUser();
        Document document = visible(key);
        if (document.getOwnerId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Default content can't be changed. Copy it into your own document to customize it.");
        }
        if (document.getOwnerId() == user || role(key, user).filter(EDITOR::equals).isPresent()) {
            return document;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can view document '" + key + "' but not change it");
    }

    /** A document the current user owns: needed to change the document itself or its members. */
    public Document owned(String key) {
        long user = requireUser();
        Document document = visible(key);
        if (!Objects.equals(document.getOwnerId(), user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the owner of document '" + key + "' can do that");
        }
        return document;
    }

    /** The current user's personal homebrew document, created on first use. */
    public Document personal() {
        long user = requireUser();
        String key = Keys.userDocument(user, "homebrew");
        Document document = em.find(Document.class, key);
        if (document == null) {
            String name = currentUser.username().map(username -> username + "'s homebrew").orElse("My homebrew");
            document = new Document();
            document.setKey(key);
            document.setName(name);
            document.setDisplayName(name);
            document.setType(Document.HOMEBREW);
            document.setOwnerId(user);
            em.persist(document);
        }
        return document;
    }

    /** The user's member role in a document, if any. */
    public Optional<String> role(String documentKey, long userId) {
        return jdbc.queryForList("select role from open5e.document_members where document_key = ? and user_id = ?",
                String.class, documentKey, userId).stream().findFirst();
    }
}
