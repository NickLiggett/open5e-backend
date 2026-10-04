package com.main.app.player;

import com.main.app.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Player characters. A character belongs to the user who made it (the owner), who can change anything about it and
 * delete it. It can name the user who plays it: they see it and can keep its numbers up to date (level, armor class,
 * hit points, initiative bonus, notes), but can't rename it, change its rules, or hand it on. Anyone else can't see it
 * at all, which is reported as not found.
 */
@Service
@Transactional
public class PlayerService {

    static final int MAX_OWNED = 100;
    private static final Set<String> RULESETS = Set.of("5e-2014", "5e-2024");

    private static final String SELECT = """
            select p.id, p.name, p.ruleset, p.class_key, p.class_name, p.species_key, p.species_name, p.level,
                   p.armor_class, p.hit_points, p.initiative_bonus, p.notes, p.owner_id, o.username as owner,
                   pb.username as played_by, p.created_at, p.updated_at
            from open5e.player_characters p
            join open5e.users o on o.id = p.owner_id
            left join open5e.users pb on pb.id = p.played_by
            """;

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;

    public PlayerService(CurrentUser currentUser, JdbcTemplate jdbc) {
        this.currentUser = currentUser;
        this.jdbc = jdbc;
    }

    /** The characters the current user owns or plays, by name. */
    @Transactional(readOnly = true)
    public List<PlayerDTO> list() {
        long me = requireUser();
        return jdbc.query(SELECT + " where p.owner_id = ? or p.played_by = ? order by lower(p.name), p.id",
                mapper(me), me, me);
    }

    @Transactional(readOnly = true)
    public PlayerDTO get(long id) {
        return visible(id, requireUser());
    }

    public PlayerDTO create(PlayerRequest request) {
        long me = requireUser();
        Clean clean = clean(request);
        Integer owned = jdbc.queryForObject("select count(*) from open5e.player_characters where owner_id = ?",
                Integer.class, me);
        if (owned != null && owned >= MAX_OWNED) {
            throw badRequest("You can have up to " + MAX_OWNED + " players; delete some first");
        }
        Long playedBy = playedBy(clean.playedBy(), me);
        long id = jdbc.queryForObject("""
                insert into open5e.player_characters (owner_id, played_by, name, ruleset, class_key, class_name,
                    species_key, species_name, level, armor_class, hit_points, initiative_bonus, notes)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) returning id""", Long.class,
                me, playedBy, clean.name(), clean.ruleset(), clean.classKey(), clean.className(), clean.speciesKey(),
                clean.speciesName(), clean.level(), clean.armorClass(), clean.hitPoints(), clean.initiativeBonus(),
                clean.notes());
        return visible(id, me);
    }

    /** Replaces the character. Whoever only plays it must leave the owner's parts as they are. */
    public PlayerDTO update(long id, PlayerRequest request) {
        long me = requireUser();
        PlayerDTO existing = visible(id, me);
        Clean clean = clean(request);
        if (existing.role().equals("OWNER")) {
            jdbc.update("""
                    update open5e.player_characters set played_by = ?, name = ?, ruleset = ?, class_key = ?,
                        class_name = ?, species_key = ?, species_name = ?, level = ?, armor_class = ?, hit_points = ?,
                        initiative_bonus = ?, notes = ?, updated_at = now() where id = ?""",
                    playedBy(clean.playedBy(), me), clean.name(), clean.ruleset(), clean.classKey(),
                    clean.className(), clean.speciesKey(), clean.speciesName(), clean.level(), clean.armorClass(),
                    clean.hitPoints(), clean.initiativeBonus(), clean.notes(), id);
        } else {
            boolean ownersPartsChanged = !Objects.equals(clean.name(), existing.name())
                    || !Objects.equals(clean.ruleset(), existing.ruleset())
                    || !Objects.equals(clean.classKey(), existing.classKey())
                    || !Objects.equals(clean.className(), existing.className())
                    || !Objects.equals(clean.speciesKey(), existing.speciesKey())
                    || !Objects.equals(clean.speciesName(), existing.speciesName())
                    || !Objects.equals(clean.playedBy(), existing.playedBy());
            if (ownersPartsChanged) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only " + existing.owner()
                        + " can change the name, rules, class, species or player. You can change the level, armor "
                        + "class, hit points, initiative bonus and notes.");
            }
            jdbc.update("""
                    update open5e.player_characters set level = ?, armor_class = ?, hit_points = ?,
                        initiative_bonus = ?, notes = ?, updated_at = now() where id = ?""",
                    clean.level(), clean.armorClass(), clean.hitPoints(), clean.initiativeBonus(), clean.notes(), id);
        }
        return visible(id, me);
    }

    /** Owner only. */
    public void delete(long id) {
        long me = requireUser();
        PlayerDTO existing = visible(id, me);
        if (!existing.role().equals("OWNER")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only " + existing.owner() + " can delete this player");
        }
        jdbc.update("delete from open5e.player_characters where id = ?", id);
    }

    private PlayerDTO visible(long id, long me) {
        return jdbc.query(SELECT + " where p.id = ? and (p.owner_id = ? or p.played_by = ?)", mapper(me), id, me, me)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No player " + id));
    }

    private static RowMapper<PlayerDTO> mapper(long me) {
        return (rs, row) -> new PlayerDTO(rs.getLong("id"), rs.getString("name"), rs.getString("ruleset"),
                rs.getString("class_key"), rs.getString("class_name"), rs.getString("species_key"),
                rs.getString("species_name"), rs.getInt("level"), rs.getObject("armor_class", Integer.class),
                rs.getObject("hit_points", Integer.class), rs.getInt("initiative_bonus"), rs.getString("notes"),
                rs.getString("owner"), rs.getString("played_by"), rs.getLong("owner_id") == me ? "OWNER" : "PLAYER",
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant());
    }

    /** The id of the user who plays the character; none if it's the owner, who needs no special mention. */
    private Long playedBy(String username, long owner) {
        if (username == null) {
            return null;
        }
        long id = jdbc.queryForList("select id from open5e.users where username = ?", Long.class, username).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user '" + username + "'"));
        return id == owner ? null : id;
    }

    private long requireUser() {
        return currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to keep players"));
    }

    /** The request, checked and tidied. */
    private record Clean(String name, String ruleset, String classKey, String className, String speciesKey,
                         String speciesName, int level, Integer armorClass, Integer hitPoints, int initiativeBonus,
                         String notes, String playedBy) {
    }

    private static Clean clean(PlayerRequest request) {
        if (request == null) {
            throw badRequest("A player needs a name and rules");
        }
        String name = text(request.name(), "name", 60);
        if (name == null) {
            throw badRequest("A player needs a name");
        }
        String ruleset = request.ruleset() == null ? null : request.ruleset().trim().toLowerCase(Locale.ROOT);
        if (ruleset == null || !RULESETS.contains(ruleset)) {
            throw badRequest("ruleset must be 5e-2014 or 5e-2024");
        }
        int level = request.level() == null ? 1 : request.level();
        if (level < 1 || level > 20) {
            throw badRequest("level must be 1 to 20");
        }
        int bonus = request.initiativeBonus() == null ? 0 : request.initiativeBonus();
        if (bonus < -10 || bonus > 30) {
            throw badRequest("initiativeBonus must be -10 to 30");
        }
        String playedBy = text(request.playedBy(), "playedBy", 32);
        if (playedBy != null) {
            playedBy = playedBy.replaceFirst("^@+", "").toLowerCase(Locale.ROOT);
            playedBy = playedBy.isEmpty() ? null : playedBy;
        }
        return new Clean(name, ruleset, text(request.classKey(), "classKey", 120), text(request.className(), "className", 80),
                text(request.speciesKey(), "speciesKey", 120), text(request.speciesName(), "speciesName", 80), level,
                range(request.armorClass(), "armorClass", 0, 40), range(request.hitPoints(), "hitPoints", 0, 9999), bonus,
                text(request.notes(), "notes", 5000), playedBy);
    }

    /** Trimmed text, null if there is none. */
    private static String text(String value, String field, int max) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.length() > max) {
            throw badRequest(field + " can be up to " + max + " characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Integer range(Integer value, String field, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw badRequest(field + " must be " + min + " to " + max);
        }
        return value;
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
