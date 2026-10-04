package com.main.app.player;

import com.main.app.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * Parties. The current user, as a DM, asks users to join their party and can remove them; as a player, they accept or
 * leave the parties they're asked to join. Only accepted members count: a pending invitation shows the DM nothing.
 */
@Service
@Transactional
public class PartyService {

    static final int MAX_MEMBERS = 30;

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;

    public PartyService(CurrentUser currentUser, JdbcTemplate jdbc) {
        this.currentUser = currentUser;
        this.jdbc = jdbc;
    }

    /** The users in the current user's party, and those asked who haven't answered. */
    @Transactional(readOnly = true)
    public List<PartyMemberDTO> members() {
        return jdbc.query("""
                select u.username, m.status from open5e.party_members m join open5e.users u on u.id = m.user_id
                where m.dm_id = ? order by u.username""",
                (rs, row) -> new PartyMemberDTO(rs.getString("username"), rs.getString("status")), requireUser());
    }

    /** Asks a user to join the party. Asking again changes nothing. */
    public PartyMemberDTO invite(String username) {
        long me = requireUser();
        long user = userId(username);
        if (user == me) {
            throw badRequest("You can't add yourself; your own players are already yours");
        }
        Integer count = jdbc.queryForObject("select count(*) from open5e.party_members where dm_id = ? and user_id <> ?",
                Integer.class, me, user);
        if (count != null && count >= MAX_MEMBERS) {
            throw badRequest("A party can have up to " + MAX_MEMBERS + " people; remove some first");
        }
        jdbc.update("insert into open5e.party_members (dm_id, user_id) values (?, ?) on conflict do nothing", me, user);
        String status = jdbc.queryForObject("select status from open5e.party_members where dm_id = ? and user_id = ?",
                String.class, me, user);
        return new PartyMemberDTO(normalize(username), status);
    }

    /** Removes a user from the party, or withdraws the request. */
    public void remove(String username) {
        long me = requireUser();
        if (jdbc.update("delete from open5e.party_members where dm_id = ? and user_id = ?", me, userId(username)) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "'" + normalize(username) + "' isn't in your party");
        }
    }

    /** The parties the current user has been asked to join or is in. */
    @Transactional(readOnly = true)
    public List<PartyInvitationDTO> invitations() {
        return jdbc.query("""
                select u.username, m.status from open5e.party_members m join open5e.users u on u.id = m.dm_id
                where m.user_id = ? order by u.username""",
                (rs, row) -> new PartyInvitationDTO(rs.getString("username"), rs.getString("status")), requireUser());
    }

    /** Joins a party they were asked to join. */
    public PartyInvitationDTO accept(String dm) {
        long me = requireUser();
        if (jdbc.update("update open5e.party_members set status = 'ACCEPTED' where dm_id = ? and user_id = ?",
                userId(dm), me) == 0) {
            throw notAsked(dm);
        }
        return new PartyInvitationDTO(normalize(dm), "ACCEPTED");
    }

    /** Turns down a request, or leaves a party. Their players stay theirs; the DM just stops seeing them. */
    public void leave(String dm) {
        long me = requireUser();
        if (jdbc.update("delete from open5e.party_members where dm_id = ? and user_id = ?", userId(dm), me) == 0) {
            throw notAsked(dm);
        }
    }

    private static ResponseStatusException notAsked(String dm) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "'" + normalize(dm) + "' hasn't asked you to join a party");
    }

    private long userId(String username) {
        String name = normalize(username);
        return jdbc.queryForList("select id from open5e.users where username = ?", Long.class, name).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user '" + name + "'"));
    }

    private static String normalize(String username) {
        return username == null ? "" : username.strip().replaceFirst("^@+", "").toLowerCase(Locale.ROOT);
    }

    private long requireUser() {
        return currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to use a party"));
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
