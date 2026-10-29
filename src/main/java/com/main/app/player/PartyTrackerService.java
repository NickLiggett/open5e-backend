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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lets people in a DM's party see the DM's initiative tracker, for the fights their characters are in.
 *
 * <p>A tracker is the DM's own and can hold what the table must not see (a monster's hit points, which monster it is),
 * so it is never given as it is kept: this builds a new view from the fields a player may see, and nothing else. A
 * monster's name, armor class and hit points show only while the DM lets them (each can be hidden on its own: {@code nameHidden}, and {@code acRevealed} and {@code hpRevealed}, which follow the older all-or-nothing {@code revealed} when absent); characters' always show.
 */
@Service
@Transactional(readOnly = true)
public class PartyTrackerService {

    /** A tracker as kept: {@code trackerId} and {@code name} are null for the one in use (its name is in its state). */
    private record Saved(String dm, Long trackerId, String name, Instant updatedAt, String state) {
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
     * (owned or played) is: each of a DM's trackers, the one in use and those put aside, is its own entry. Pending invitations and other people's trackers show nothing.
     */
    public List<PartyTrackerDTO> trackers() {
        long me = currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to see your party's tracker"));
        Set<Long> mine = new HashSet<>(jdbc.queryForList(
                "select id from open5e.player_characters where owner_id = ? or played_by = ?", Long.class, me, me));
        if (mine.isEmpty()) {
            return List.of();
        }
        List<Saved> saved = new ArrayList<>(jdbc.query("""
                select u.username, t.updated_at, t.state::text as state
                from open5e.party_members m
                join open5e.users u on u.id = m.dm_id
                join open5e.user_tracker_states t on t.user_id = m.dm_id
                where m.user_id = ? and m.status = 'ACCEPTED'""",
                (rs, row) -> new Saved(rs.getString("username"), null, null, rs.getTimestamp("updated_at").toInstant(), rs.getString("state")),
                me));
        saved.addAll(jdbc.query("""
                select u.username, t.id, t.name, t.updated_at, t.state::text as state
                from open5e.party_members m
                join open5e.users u on u.id = m.dm_id
                join open5e.user_saved_trackers t on t.user_id = m.dm_id
                where m.user_id = ? and m.status = 'ACCEPTED'""",
                (rs, row) -> new Saved(rs.getString("username"), rs.getLong("id"), rs.getString("name"),
                        rs.getTimestamp("updated_at").toInstant(), rs.getString("state")),
                me));
        // by DM, the one in use first, then the most recently saved
        saved.sort(Comparator.comparing(Saved::dm).thenComparing(one -> one.trackerId() != null)
                .thenComparing(Saved::updatedAt, Comparator.reverseOrder()));

        List<PartyTrackerDTO> result = new ArrayList<>();
        for (Saved one : saved) {
            JsonNode state = jsonMapper.readTree(one.state());
            List<PartyTrackerDTO.Combatant> combatants = view(state, mine);
            if (combatants.stream().anyMatch(PartyTrackerDTO.Combatant::mine)) {
                String name = one.trackerId() != null ? one.name() : state.path("name").isString() ? state.path("name").asString() : "";
                result.add(new PartyTrackerDTO(one.dm(), one.trackerId(), name, one.trackerId() == null, one.updatedAt(), combatants));
            }
        }
        return result;
    }

    /** The turn order as a player sees it, from the DM's saved state. Anything not understood is left out. */
    private List<PartyTrackerDTO.Combatant> view(JsonNode state, Set<Long> mine) {
        JsonNode rows = state.path("combatants");
        List<PartyTrackerDTO.Combatant> combatants = new ArrayList<>();
        if (!rows.isArray()) {
            return combatants;
        }
        for (JsonNode row : rows) {
            Long playerId = whole(row.path("playerId"));
            String type = row.path("type").isString() ? row.path("type").asString() : "";
            boolean character = playerId != null || "PC".equals(type); // characters are the table's own
            boolean revealed = flag(row, "revealed", false); // both armor class and hit points, as rows were once all-or-nothing
            boolean acShown = character || flag(row, "acRevealed", revealed);
            boolean hpShown = character || flag(row, "hpRevealed", revealed);
            boolean nameShown = character || !flag(row, "nameHidden", false);
            combatants.add(new PartyTrackerDTO.Combatant(
                    whole(row.path("id")),
                    nameShown ? (row.path("name").isString() ? row.path("name").asString() : "") : null,
                    small(row.path("initiative")),
                    type,
                    playerId,
                    playerId != null && mine.contains(playerId),
                    acShown ? small(row.path("ac")) : null,
                    hpShown ? small(row.path("hp")) : null,
                    acShown && hpShown && !character));
        }
        return combatants;
    }

    /** A true-or-false field of a row, or {@code otherwise} when it isn't there. */
    private static boolean flag(JsonNode row, String field, boolean otherwise) {
        return row.path(field).isBoolean() ? row.path(field).asBoolean() : otherwise;
    }

    private static Long whole(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToLong() ? node.longValue() : null;
    }

    private static Integer small(JsonNode node) {
        return node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }
}
