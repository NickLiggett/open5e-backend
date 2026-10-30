package com.main.app.player;

import com.main.app.user.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Player characters. A character belongs to the user who made it (the owner), who can change anything about it and
 * delete it. It can name the user who plays it: they see it and can keep its numbers up to date (level, armor class,
 * hit points, initiative bonus, notes), but can't rename it, change its rules, or hand it on. A DM whose party the
 * owner or the player has accepted can see it and nothing more. Anyone else can't see it at all, which is reported as
 * not found.
 */
@Service
@Transactional
public class PlayerService {

    static final int MAX_OWNED = 100;
    /** The most classes one character can have; the levels in them add up to 20 at most, so this is never the limit in play. */
    static final int MAX_CLASSES = 12;
    private static final Set<String> RULESETS = Set.of("5e-2014", "5e-2024");
    private static final TypeReference<List<ClassLevel>> CLASS_LIST = new TypeReference<>() {
    };

    private static final String SELECT = """
            select p.id, p.name, p.ruleset, p.class_key, p.class_name, p.classes, p.species_key, p.species_name, p.level,
                   p.armor_class, p.hit_points, p.initiative_bonus, p.notes, p.owner_id, p.played_by as played_by_id,
                   o.username as owner, pb.username as played_by, p.created_at, p.updated_at
            from open5e.player_characters p
            join open5e.users o on o.id = p.owner_id
            left join open5e.users pb on pb.id = p.played_by
            """;

    /** Whether the user asking (the {@code ?}) has the owner or the player of the character in their party. */
    private static final String IN_MY_PARTY = """
            exists (select 1 from open5e.party_members m where m.dm_id = ? and m.status = 'ACCEPTED'
                    and (m.user_id = p.owner_id or m.user_id = p.played_by))""";

    private final CurrentUser currentUser;
    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    public PlayerService(CurrentUser currentUser, JdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.currentUser = currentUser;
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    /** The characters the current user owns or plays, by name. */
    @Transactional(readOnly = true)
    public List<PlayerDTO> list() {
        long me = requireUser();
        return jdbc.query(SELECT + " where p.owner_id = ? or p.played_by = ? order by lower(p.name), p.id",
                mapper(me), me, me);
    }

    /** The characters of the people in the current user's party, other than their own, by name. */
    @Transactional(readOnly = true)
    public List<PlayerDTO> party() {
        long me = requireUser();
        return jdbc.query(SELECT + " where p.owner_id <> ? and (p.played_by is null or p.played_by <> ?) and "
                + IN_MY_PARTY + " order by lower(p.name), p.id", mapper(me), me, me, me);
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
        Long playedBy = playedBy(clean.playedBy());
        long id = jdbc.queryForObject("""
                insert into open5e.player_characters (owner_id, played_by, name, ruleset, class_key, class_name,
                    classes, species_key, species_name, level, armor_class, hit_points, initiative_bonus, notes)
                values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?, ?, ?, ?) returning id""", Long.class,
                me, playedBy, clean.name(), clean.ruleset(), clean.classKey(), clean.className(), classesJson(clean),
                clean.speciesKey(), clean.speciesName(), clean.level(), clean.armorClass(), clean.hitPoints(),
                clean.initiativeBonus(), clean.notes());
        return visible(id, me);
    }

    /** Replaces the character. Whoever only plays it must leave the owner's parts as they are. */
    public PlayerDTO update(long id, PlayerRequest request) {
        long me = requireUser();
        PlayerDTO existing = visible(id, me);
        Clean clean = clean(request);
        if (existing.role().equals("PARTY")) {
            throw readOnly(existing);
        }
        if (existing.role().equals("OWNER")) {
            jdbc.update("""
                    update open5e.player_characters set played_by = ?, name = ?, ruleset = ?, class_key = ?,
                        class_name = ?, classes = ?::jsonb, species_key = ?, species_name = ?, level = ?,
                        armor_class = ?, hit_points = ?, initiative_bonus = ?, notes = ?, updated_at = now()
                    where id = ?""",
                    playedBy(clean.playedBy()), clean.name(), clean.ruleset(), clean.classKey(),
                    clean.className(), classesJson(clean), clean.speciesKey(), clean.speciesName(), clean.level(),
                    clean.armorClass(), clean.hitPoints(), clean.initiativeBonus(), clean.notes(), id);
        } else {
            boolean ownersPartsChanged = !Objects.equals(clean.name(), existing.name())
                    || !Objects.equals(clean.ruleset(), existing.ruleset())
                    || !Objects.equals(whichClasses(clean.classes()), whichClasses(existing.classes()))
                    || !Objects.equals(clean.speciesKey(), existing.speciesKey())
                    || !Objects.equals(clean.speciesName(), existing.speciesName())
                    || !Objects.equals(clean.playedBy(), existing.playedBy());
            if (ownersPartsChanged) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only " + existing.owner()
                        + " can change the name, rules, class, species or player. You can change the level, armor "
                        + "class, hit points, initiative bonus and notes.");
            }
            jdbc.update("""
                    update open5e.player_characters set level = ?, classes = ?::jsonb, armor_class = ?, hit_points = ?,
                        initiative_bonus = ?, notes = ?, updated_at = now() where id = ?""",
                    clean.level(), classesJson(clean), clean.armorClass(), clean.hitPoints(), clean.initiativeBonus(),
                    clean.notes(), id);
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
        return jdbc.query(SELECT + " where p.id = ? and (p.owner_id = ? or p.played_by = ? or " + IN_MY_PARTY + ")",
                        mapper(me), id, me, me, me)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No player " + id));
    }

    private RowMapper<PlayerDTO> mapper(long me) {
        return (rs, row) -> new PlayerDTO(rs.getLong("id"), rs.getString("name"), rs.getString("ruleset"),
                rs.getString("class_key"), rs.getString("class_name"), rs.getString("species_key"),
                rs.getString("species_name"), rs.getInt("level"), rs.getObject("armor_class", Integer.class),
                rs.getObject("hit_points", Integer.class), rs.getInt("initiative_bonus"), rs.getString("notes"),
                rs.getString("owner"), rs.getString("played_by"), role(rs.getLong("owner_id"), rs.getObject("played_by_id", Long.class), me),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                jsonMapper.readValue(rs.getString("classes"), CLASS_LIST));
    }

    private String classesJson(Clean clean) {
        return jsonMapper.writeValueAsString(clean.classes());
    }

    /** The classes without their levels: which classes a character has, which only its owner can change. */
    private static List<List<String>> whichClasses(List<ClassLevel> classes) {
        return classes.stream().map(one -> Arrays.asList(one.classKey(), one.className())).toList();
    }

    private static String role(long ownerId, Long playedById, long me) {
        if (ownerId == me) {
            return "OWNER";
        }
        return playedById != null && playedById == me ? "PLAYER" : "PARTY";
    }

    private static ResponseStatusException readOnly(PlayerDTO player) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can see " + player.name() + " because they're in your party, but not change it");
    }

    /** The id of the user who plays the character, who may be the owner; none if no one is named. */
    private Long playedBy(String username) {
        if (username == null) {
            return null;
        }
        return jdbc.queryForList("select id from open5e.users where username = ?", Long.class, username).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No user '" + username + "'"));
    }

    private long requireUser() {
        return currentUser.id().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to keep players"));
    }

    /** The request, checked and tidied. */
    private record Clean(String name, String ruleset, String classKey, String className, List<ClassLevel> classes,
                         String speciesKey, String speciesName, int level, Integer armorClass, Integer hitPoints,
                         int initiativeBonus, String notes, String playedBy) {
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
        List<ClassLevel> classes = classes(request, level);
        if (!classes.isEmpty()) {
            level = classes.stream().mapToInt(ClassLevel::level).sum();
            if (level > 20) {
                throw badRequest("The levels in all the classes add up to " + level + "; 20 is the most");
            }
        }
        ClassLevel first = classes.isEmpty() ? null : classes.get(0);
        int bonus = request.initiativeBonus() == null ? 0 : request.initiativeBonus();
        if (bonus < -10 || bonus > 30) {
            throw badRequest("initiativeBonus must be -10 to 30");
        }
        String playedBy = text(request.playedBy(), "playedBy", 32);
        if (playedBy != null) {
            playedBy = playedBy.replaceFirst("^@+", "").toLowerCase(Locale.ROOT);
            playedBy = playedBy.isEmpty() ? null : playedBy;
        }
        return new Clean(name, ruleset, first == null ? null : first.classKey(), first == null ? null : first.className(),
                classes, text(request.speciesKey(), "speciesKey", 120), text(request.speciesName(), "speciesName", 80),
                level, range(request.armorClass(), "armorClass", 0, 40), range(request.hitPoints(), "hitPoints", 0, 9999),
                bonus, text(request.notes(), "notes", 5000), playedBy);
    }

    /**
     * The character's classes, first class first. A request without {@code classes} has the one class its
     * {@code className} names, at its {@code level}, or none.
     */
    private static List<ClassLevel> classes(PlayerRequest request, int level) {
        List<ClassLevel> given = request.classes();
        if (given == null || given.isEmpty()) {
            String className = text(request.className(), "className", 80);
            return className == null ? List.of()
                    : List.of(new ClassLevel(text(request.classKey(), "classKey", 120), className, level));
        }
        if (given.size() > MAX_CLASSES) {
            throw badRequest("A character can have up to " + MAX_CLASSES + " classes");
        }
        List<ClassLevel> classes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ClassLevel one : given) {
            String className = one == null ? null : text(one.className(), "className", 80);
            if (className == null) {
                throw badRequest("Each class needs a name");
            }
            if (!seen.add(className.toLowerCase(Locale.ROOT))) {
                throw badRequest(className + " is listed twice; give it one level count");
            }
            if (one.level() < 1 || one.level() > 20) {
                throw badRequest("The level in " + className + " must be 1 to 20");
            }
            classes.add(new ClassLevel(text(one.classKey(), "classKey", 120), className, one.level()));
        }
        return classes;
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
