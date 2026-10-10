package com.main.app.player;

import com.main.app.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets people in a DM's party see the DM's initiative tracker, for the fights their characters are in.
 *
 * <p>A tracker is the DM's own and can hold what the table must not see (a monster's hit points, which monster it is),
 * so it is never given as it is kept: this builds a new view from the fields a player may see, and nothing else. A
 * monster's armor class and hit points show only once the DM has revealed that row; characters' always show.
 */
@Service
@Transactional(readOnly = true)
public class PartyTrackerService {

    private record Saved(String dm, Instant updatedAt, String state) {
    }

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    public PartyTrackerService(CurrentUser currentUser, JdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.currentUser = currentUser;
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    /**
     * The trackers of the DMs whose party the current user has joined and in whose turn order one of their characters
     * (owned or played) is, one for each DM. Pending invitations and other people's trackers show nothing.
     */
    public List<PartyTrackerDTO> trackers() {
        long me = currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to see your party's tracker"));
        Set<Long> mine = new HashSet<>(jdbc.queryForList(
                "select id from open5e.player_characters where owner_id = ? or played_by = ?", Long.class, me, me));
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Saved> saved = jdbc.query("""
                select u.username, t.updated_at, t.state::text as state
                from open5e.party_members m
                join open5e.users u on u.id = m.dm_id
                join open5e.user_tracker_states t on t.user_id = m.dm_id
                where m.user_id = ? and m.status = 'ACCEPTED' order by u.username""",
                (rs, row) -> new Saved(rs.getString("username"), rs.getTimestamp("updated_at").toInstant(), rs.getString("state")),
                me);

        List<PartyTrackerDTO> result = new ArrayList<>();
        for (Saved one : saved) {
            List<PartyTrackerDTO.Combatant> combatants = view(one.state(), mine);
            if (combatants.stream().anyMatch(PartyTrackerDTO.Combatant::mine)) {
                result.add(new PartyTrackerDTO(one.dm(), one.updatedAt(), combatants));
            }
        }
        return result;
    }

    /** The turn order as a player sees it, from the DM's saved state. Anything not understood is left out. */
    private List<PartyTrackerDTO.Combatant> view(String state, Set<Long> mine) {
        JsonNode rows = jsonMapper.readTree(state).path("combatants");
        List<PartyTrackerDTO.Combatant> combatants = new ArrayList<>();
        if (!rows.isArray()) {
            return combatants;
        }
        for (JsonNode row : rows) {
            Long playerId = whole(row.path("playerId"));
            String type = row.path("type").isString() ? row.path("type").asString() : "";
            boolean revealed = row.path("revealed").isBoolean() && row.path("revealed").asBoolean();
            boolean known = playerId != null || "PC".equals(type) || revealed; // characters are the table's own
            combatants.add(new PartyTrackerDTO.Combatant(
                    whole(row.path("id")),
                    row.path("name").isString() ? row.path("name").asString() : "",
                    small(row.path("initiative")),
                    type,
                    playerId,
                    playerId != null && mine.contains(playerId),
                    known ? small(row.path("ac")) : null,
                    known ? small(row.path("hp")) : null,
                    revealed));
        }
        return combatants;
    }

    private static Long whole(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToLong() ? node.longValue() : null;
    }

    private static Integer small(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }
}
