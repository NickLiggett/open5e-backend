package com.main.app.document;

import com.main.app.ownership.Keys;
import com.main.app.ownership.OwnedResource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Users' own documents and who they're shared with. Permissions come from {@link DocumentAccess}. */
@Service
@Transactional
public class DocumentService {

    private static final Set<String> ROLES = Set.of(DocumentAccess.VIEWER, DocumentAccess.EDITOR);

    private final EntityManager em;
    private final DocumentAccess access;
    private final JdbcTemplate jdbc;

    public DocumentService(EntityManager em, DocumentAccess access, JdbcTemplate jdbc) {
        this.em = em;
        this.access = access;
        this.jdbc = jdbc;
    }

    public Document create(DocumentRequest request) {
        long user = access.requireUser();
        String name = requireName(request);
        String slug = Keys.slugify(request.slug() != null ? request.slug() : name);
        if (slug.isEmpty()) {
            throw badRequest("Give the document a name or a slug (letters or digits)");
        }
        String key = Keys.userDocument(user, slug);
        if (em.find(Document.class, key) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "You already have a document with key '" + key + "'; use another name or slug");
        }
        Document document = new Document();
        document.setKey(key);
        document.setType(Document.HOMEBREW);
        document.setOwnerId(user);
        setFields(document, request, name);
        em.persist(document);
        return document;
    }

    /** Replaces the document's name, display name, description and author. Its key doesn't change. */
    public Document update(String key, DocumentRequest request) {
        Document document = access.owned(key);
        setFields(document, request, requireName(request));
        return document;
    }

    /** Deletes the document and everything in it. Copies other users made of its content keep their data. */
    public void delete(String key) {
        Document document = access.owned(key);
        for (EntityType<?> entity : em.getMetamodel().getEntities()) {
            if (OwnedResource.class.isAssignableFrom(entity.getJavaType())) {
                em.createQuery("delete from " + entity.getName() + " e where e.documentKey = :key")
                        .setParameter("key", key)
                        .executeUpdate();
            }
        }
        em.remove(document); // members are deleted with it (ON DELETE CASCADE)
    }

    /** The owner and members of a document the current user can see. Default content has none. */
    @Transactional(readOnly = true)
    public List<MemberDTO> members(String key) {
        Document document = access.visible(key);
        List<MemberDTO> members = new ArrayList<>();
        if (document.getOwnerId() != null) {
            members.add(new MemberDTO(username(document.getOwnerId()), "OWNER"));
        }
        members.addAll(jdbc.query("""
                        select u.username, m.role from open5e.document_members m join open5e.users u on u.id = m.user_id
                        where m.document_key = ? order by u.username""",
                (rs, row) -> new MemberDTO(rs.getString("username"), rs.getString("role")), key));
        return members;
    }

    /** Shares the document with a user, or changes their role. Owner only. */
    public MemberDTO share(String key, String username, MemberRequest request) {
        Document document = access.owned(key);
        String role = request == null || request.role() == null ? null : request.role().toUpperCase(Locale.ROOT);
        if (role == null || !ROLES.contains(role)) {
            throw badRequest("role must be VIEWER or EDITOR");
        }
        long member = userId(username);
        if (member == document.getOwnerId()) {
            throw badRequest("The owner already has full access");
        }
        jdbc.update("""
                insert into open5e.document_members (document_key, user_id, role) values (?, ?, ?)
                on conflict (document_key, user_id) do update set role = excluded.role""", key, member, role);
        return new MemberDTO(username, role);
    }

    /** Stops sharing the document with a user. The owner can remove anyone; members can remove themselves. */
    public void unshare(String key, String username) {
        long user = access.requireUser();
        Document document = access.visible(key);
        long member = userId(username);
        if (member != user) {
            access.owned(key);
        }
        if (document.getOwnerId() != null && member == document.getOwnerId()) {
            throw badRequest("The owner can't be removed; delete the document instead");
        }
        jdbc.update("delete from open5e.document_members where document_key = ? and user_id = ?", key, member);
    }

    private static void setFields(Document document, DocumentRequest request, String name) {
        document.setName(name);
        document.setDisplayName(request.displayName() != null ? request.displayName() : name);
        document.setDesc(request.desc());
        document.setAuthor(request.author());
    }

    private static String requireName(DocumentRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw badRequest("A document needs a name");
        }
        return request.name().trim();
    }

    private long userId(String username) {
        return jdbc.queryForList("select id from open5e.users where username = ?", Long.class, username).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user '" + username + "'"));
    }

    private String username(long id) {
        return jdbc.queryForObject("select username from open5e.users where id = ?", String.class, id);
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
