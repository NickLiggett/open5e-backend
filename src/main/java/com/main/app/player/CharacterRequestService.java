package com.main.app.player;

import com.main.app.user.CurrentUser;
import com.main.app.user.Emails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Asking someone to play a character. Nobody can be made the player of a character: its owner asks, by email address,
 * and the person becomes the player only by accepting, signed in with that address <em>verified</em> (see
 * {@code UserService}). It isn't a link anyone could pass on, and what the answer to the owner reveals doesn't depend on
 * whether the address has an account. A character has one request at a time. A denied one is kept until the owner
 * dismisses it, and its address can't be asked again for {@value #COOLDOWN_DAYS} days.
 */
@Service
public class CharacterRequestService {

    static final int VALID_DAYS = 30;
    static final int COOLDOWN_DAYS = 7;
    static final int MAX_PENDING = 20;

    private static final Logger log = LoggerFactory.getLogger(CharacterRequestService.class);

    private static final String SENT = """
            select r.id, r.player_id, p.name, r.email, r.status, r.created_at, r.expires_at, r.responded_at
            from open5e.character_requests r join open5e.player_characters p on p.id = r.player_id""";

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final CharacterRequestMailer mailer;

    public CharacterRequestService(CurrentUser currentUser, JdbcTemplate jdbc, TransactionTemplate transaction,
                                   CharacterRequestMailer mailer) {
        this.currentUser = currentUser;
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.mailer = mailer;
    }

    /**
     * Asks an address to play a character the current user owns, and emails it. Asking the same address again while
     * the request is waiting changes nothing and sends nothing; asking another replaces the request.
     */
    public CharacterRequestDTO ask(long playerId, CharacterRequestBody body) {
        String email = Emails.normalize(body == null ? null : body.email());
        if (email == null) {
            throw badRequest("That doesn't look like an email address");
        }
        Asked asked = transaction.execute(status -> create(playerId, email));
        boolean sent = false;
        if (asked.send()) {
            try { // outside the transaction: a slow or broken mail server mustn't hold the database
                mailer.send(email, asked.owner(), asked.characterName());
                sent = true;
            } catch (RuntimeException e) {
                log.warn("Couldn't email the character request to {}: {}", email, e.toString());
            }
        }
        CharacterRequestDTO made = asked.request();
        return new CharacterRequestDTO(made.id(), made.playerId(), made.characterName(), made.email(), made.status(),
                made.createdAt(), made.expiresAt(), made.respondedAt(), made.askAgainAt(), sent);
    }

    /** The requests the current user has made that are still waiting or were turned down, newest first. */
    public List<CharacterRequestDTO> sent() {
        return jdbc.query(SENT + """
                 where r.requested_by = ? and (r.status = 'DENIED' or r.expires_at > now())
                order by r.created_at desc, r.id desc""", sentMapper(), requireUser());
    }

    /** Withdraws a request that's waiting, or dismisses one that was turned down. */
    public void cancel(long id) {
        if (jdbc.update("delete from open5e.character_requests where id = ? and requested_by = ?", id, requireUser()) == 0) {
            throw notFound(id);
        }
    }

    /** The requests to play a character that were sent to the current user's verified address and wait for an answer. */
    public List<IncomingCharacterRequestDTO> received() {
        long me = requireUser();
        return jdbc.query("""
                select r.id, p.name, p.class_name, p.species_name, p.level, o.username as owner, r.created_at, r.expires_at
                from open5e.character_requests r
                join open5e.player_characters p on p.id = r.player_id
                join open5e.users o on o.id = r.requested_by
                where r.status = 'PENDING' and r.expires_at > now() and r.email = (select verified_email from open5e.users where id = ?)
                order by r.created_at desc, r.id desc""",
                (rs, row) -> new IncomingCharacterRequestDTO(rs.getLong("id"), rs.getString("name"),
                        rs.getString("class_name"), rs.getString("species_name"), rs.getInt("level"),
                        rs.getString("owner"), rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("expires_at").toInstant()), me);
    }

    /** Plays the character. It replaces whoever played it before, and the request is done with. */
    public void accept(long id) {
        transaction.executeWithoutResult(status -> {
            Incoming incoming = incoming(id);
            // the version moves so that an owner's form opened before this can't quietly put the old player back
            jdbc.update("update open5e.player_characters set played_by = ?, version = version + 1, updated_at = now() where id = ?",
                    incoming.me(), incoming.playerId());
            jdbc.update("delete from open5e.character_requests where id = ?", id);
        });
    }

    /** Turns it down. The owner is shown that, and can't ask this address again for a while. */
    public void deny(long id) {
        transaction.executeWithoutResult(status -> {
            incoming(id);
            jdbc.update("update open5e.character_requests set status = 'DENIED', responded_at = now() where id = ?", id);
        });
    }

    private Asked create(long playerId, String email) {
        long me = requireUser();
        List<String> owned = jdbc.queryForList("select name from open5e.player_characters where id = ? and owner_id = ?",
                String.class, playerId, me);
        if (owned.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No player " + playerId);
        }
        String own = jdbc.queryForObject("select verified_email from open5e.users where id = ?", String.class, me);
        if (email.equals(own)) {
            throw badRequest("That's your own address; choose yourself as the player in the character's details");
        }

        List<CharacterRequestDTO> before = jdbc.query(SENT + " where r.player_id = ?", sentMapper(), playerId);
        if (!before.isEmpty()) {
            CharacterRequestDTO old = before.getFirst();
            if (old.email().equals(email)) {
                if (old.status().equals("DENIED")) {
                    if (old.askAgainAt().isAfter(Instant.now())) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, email + " turned this request down; you can ask "
                                + "that address again after " + old.askAgainAt());
                    }
                } else if (old.expiresAt().isAfter(Instant.now())) {
                    return new Asked(old, false, null, null);
                }
            }
        }

        Integer waiting = jdbc.queryForObject("""
                select count(*) from open5e.character_requests
                where requested_by = ? and status = 'PENDING' and expires_at > now() and player_id <> ?""",
                Integer.class, me, playerId);
        if (waiting != null && waiting >= MAX_PENDING) {
            throw badRequest("You can have up to " + MAX_PENDING + " requests waiting for an answer; withdraw some first");
        }

        jdbc.update("""
                insert into open5e.character_requests (player_id, requested_by, email, expires_at) values (?, ?, ?, ?)
                on conflict (player_id) do update set requested_by = excluded.requested_by, email = excluded.email,
                    status = 'PENDING', created_at = now(), expires_at = excluded.expires_at, responded_at = null""",
                playerId, me, email, Timestamp.from(Instant.now().plus(Duration.ofDays(VALID_DAYS))));
        CharacterRequestDTO made = jdbc.query(SENT + " where r.player_id = ?", sentMapper(), playerId).getFirst();
        return new Asked(made, true, currentUser.username().orElse("Someone"), owned.getFirst());
    }

    /** A request waiting for the current user, who has it only through their verified address. */
    private Incoming incoming(long id) {
        long me = requireUser();
        return jdbc.query("""
                        select r.player_id from open5e.character_requests r
                        where r.id = ? and r.status = 'PENDING' and r.expires_at > now()
                          and r.email = (select verified_email from open5e.users where id = ?)""",
                        (rs, row) -> new Incoming(rs.getLong("player_id"), me), id, me)
                .stream().findFirst().orElseThrow(() -> notFound(id));
    }

    private static RowMapper<CharacterRequestDTO> sentMapper() {
        return (rs, row) -> {
            Timestamp responded = rs.getTimestamp("responded_at");
            Instant respondedAt = responded == null ? null : responded.toInstant();
            return new CharacterRequestDTO(rs.getLong("id"), rs.getLong("player_id"), rs.getString("name"),
                    rs.getString("email"), rs.getString("status"), rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("expires_at").toInstant(), respondedAt,
                    respondedAt == null ? null : respondedAt.plus(Duration.ofDays(COOLDOWN_DAYS)), null);
        };
    }

    private record Asked(CharacterRequestDTO request, boolean send, String owner, String characterName) {
    }

    private record Incoming(long playerId, long me) {
    }

    private long requireUser() {
        return currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to ask someone to play a character"));
    }

    private static ResponseStatusException notFound(long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No request " + id);
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
