package com.main.app.document;

import com.main.app.user.CurrentUser;
import com.main.app.user.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Sharing a document with an email address. If the address belongs to a user who has verified it, they're added
 * straight away. Otherwise an invitation is kept, and accepted when someone signs in with that verified address (see
 * {@code UserService}) — it isn't a link anyone could pass on. Owner only.
 */
@Service
public class InvitationService {

    static final int VALID_DAYS = 30;
    static final int MAX_PENDING = 50;

    private static final Logger log = LoggerFactory.getLogger(InvitationService.class);
    private static final Set<String> ROLES = Set.of(DocumentAccess.VIEWER, DocumentAccess.EDITOR);

    private final DocumentAccess access;
    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final InvitationMailer mailer;

    public InvitationService(DocumentAccess access, CurrentUser currentUser, JdbcTemplate jdbc,
                             TransactionTemplate transaction, InvitationMailer mailer) {
        this.access = access;
        this.currentUser = currentUser;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.mailer = mailer;
    }

    /** Invites an address, or changes the role of a pending invitation (which also renews it and sends it again). */
    public InvitationResult invite(String key, InvitationRequest request) {
        String email = Emails.normalize(request == null ? null : request.email());
        String role = request == null || request.role() == null ? null : request.role().toUpperCase(Locale.ROOT);
        if (email == null) {
            throw badRequest("That doesn't look like an email address");
        }
        if (role == null || !ROLES.contains(role)) {
            throw badRequest("role must be VIEWER or EDITOR");
        }
        Created created = transaction.execute(status -> create(key, email, role));
        boolean sent = false;
        if (created.username() == null) {
            try { // outside the transaction: a slow or broken mail server mustn't hold the database
                mailer.send(email, created.inviter(), created.documentName(), role);
                sent = true;
            } catch (RuntimeException e) {
                log.warn("Couldn't email the invitation to {}: {}", email, e.toString());
            }
        }
        return new InvitationResult(email, role, created.username(), sent);
    }

    /** The document's pending invitations, newest first. Expired ones aren't listed. */
    public List<InvitationDTO> list(String key) {
        return transaction.execute(status -> {
            access.owned(key);
            return jdbc.query("""
                            select i.id, i.email, i.role, u.username, i.created_at, i.expires_at
                            from open5e.document_invitations i join open5e.users u on u.id = i.invited_by
                            where i.document_key = ? and i.expires_at > now() order by i.created_at desc, i.id desc""",
                    (rs, row) -> new InvitationDTO(rs.getLong("id"), rs.getString("email"), rs.getString("role"),
                            rs.getString("username"), rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("expires_at").toInstant()), key);
        });
    }

    public void cancel(String key, long id) {
        transaction.executeWithoutResult(status -> {
            access.owned(key);
            if (jdbc.update("delete from open5e.document_invitations where id = ? and document_key = ?", id, key) == 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No invitation " + id + " on document '" + key + "'");
            }
        });
    }

    private Created create(String key, String email, String role) {
        Document document = access.owned(key);
        long inviter = access.requireUser();

        Optional<Long> existing = jdbc.queryForList("select id from open5e.users where verified_email = ? order by id",
                Long.class, email).stream().findFirst();
        if (existing.isPresent()) {
            long member = existing.get();
            if (member == inviter) {
                throw badRequest("That's your own address; you already have full access");
            }
            jdbc.update("""
                    insert into open5e.document_members (document_key, user_id, role) values (?, ?, ?)
                    on conflict (document_key, user_id) do update set role = excluded.role""", key, member, role);
            String username = jdbc.queryForObject("select username from open5e.users where id = ?", String.class, member);
            return new Created(username, null, null);
        }

        Integer others = jdbc.queryForObject(
                "select count(*) from open5e.document_invitations where document_key = ? and email <> ?",
                Integer.class, key, email);
        if (others != null && others >= MAX_PENDING) {
            throw badRequest("A document can have up to " + MAX_PENDING + " pending invitations; cancel some first");
        }
        jdbc.update("""
                insert into open5e.document_invitations (document_key, email, role, invited_by, expires_at)
                values (?, ?, ?, ?, ?)
                on conflict (document_key, email) do update set role = excluded.role, invited_by = excluded.invited_by,
                    created_at = now(), expires_at = excluded.expires_at""",
                key, email, role, inviter, Timestamp.from(Instant.now().plus(Duration.ofDays(VALID_DAYS))));
        String documentName = document.getDisplayName() != null ? document.getDisplayName() : document.getName();
        return new Created(null, currentUser.username().orElse("Someone"), documentName);
    }

    private record Created(String username, String inviter, String documentName) {
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
