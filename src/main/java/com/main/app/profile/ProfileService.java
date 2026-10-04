package com.main.app.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What belongs to a user rather than to a document: their look-and-feel settings, their avatar picture, and the state
 * of their initiative tracker. Each is kept for the user it is asked for; who that is comes from the caller (see
 * {@link ProfileController}).
 */
@Service
public class ProfileService {

    /** The most the saved tracker state can be, as JSON text. */
    static final int MAX_TRACKER_CHARS = 256 * 1024;
    /** The most an avatar picture can be. The app sends 256 pixels square, which is a few kilobytes. */
    static final int MAX_AVATAR_BYTES = 512 * 1024;

    static final String AVATAR_VERSION = "avatarVersion";
    private static final Set<String> MODES = Set.of("light", "dark", "system");
    private static final Set<String> COLORS = Set.of("primary", "secondary");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    /** The picture types we keep. SVG is left out on purpose: it can carry scripts. */
    static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    /** A stored picture, and its version: when it was saved, in milliseconds since 1970. */
    public record Avatar(String contentType, byte[] image, long version) {
    }

    private final JdbcTemplate jdbc;
    private final JsonMapper jsonMapper;

    public ProfileService(JdbcTemplate jdbc, JsonMapper jsonMapper) {
        this.jdbc = jdbc;
        this.jsonMapper = jsonMapper;
    }

    // ----- settings

    /**
     * The user's settings (mode, main color, second color), as far as they have chosen any, with {@code avatarVersion}:
     * the version of their avatar picture, or null if they have none. It is worked out here and ignored if sent back.
     */
    public ObjectNode settings(long userId) {
        ObjectNode settings = stored("select settings::text from open5e.user_settings where user_id = ?", userId);
        Long version = avatarVersion(userId);
        if (version == null) {
            settings.putNull(AVATAR_VERSION);
        } else {
            settings.put(AVATAR_VERSION, version);
        }
        return settings;
    }

    /** Replaces the user's settings with the ones in the body, which must be allowed ones with allowed values. */
    public ObjectNode saveSettings(long userId, ObjectNode body) {
        ObjectNode clean = jsonMapper.createObjectNode();
        for (String name : body.propertyNames()) {
            if (name.equals(AVATAR_VERSION) || body.get(name).isNull()) {
                continue; // the first is ours to say; the second is a setting left unset
            }
            String value = body.get(name).isString() ? body.get(name).asString() : null;
            if (name.equals("mode")) {
                if (value == null || !MODES.contains(value)) {
                    throw badRequest("mode must be light, dark or system");
                }
                clean.put(name, value);
            } else if (COLORS.contains(name)) {
                if (value == null || !HEX_COLOR.matcher(value).matches()) {
                    throw badRequest(name + " must be a color like #1976d2");
                }
                clean.put(name, value.toLowerCase());
            } else {
                throw badRequest("Unknown setting '" + name + "'");
            }
        }
        upsertJson("user_settings", "settings", userId, clean);
        return settings(userId);
    }

    // ----- tracker

    /** The state the user left their tracker in: whatever JSON object they last saved, or an empty one. */
    public ObjectNode tracker(long userId) {
        return stored("select state::text from open5e.user_tracker_states where user_id = ?", userId);
    }

    /** Keeps the state of the user's tracker, which is the app's to shape; it need only be a JSON object of a sane size. */
    public ObjectNode saveTracker(long userId, ObjectNode state) {
        if (jsonMapper.writeValueAsString(state).length() > MAX_TRACKER_CHARS) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413),
                    "The saved tracker is too big: " + MAX_TRACKER_CHARS / 1024 + " KB of JSON at most");
        }
        upsertJson("user_tracker_states", "state", userId, state);
        return tracker(userId);
    }

    // ----- avatar

    /**
     * Keeps the user's avatar picture.
     *
     * @return its version
     * @throws ResponseStatusException 400 if it is empty or not the kind of picture it says, 413 if it is too big
     */
    public long saveAvatar(long userId, String contentType, byte[] image) {
        String type = baseType(contentType);
        if (!IMAGE_TYPES.contains(type)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "An avatar must be a PNG, JPEG, WebP or GIF picture");
        }
        if (image.length > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(413), "An avatar can be " + MAX_AVATAR_BYTES / 1024 + " KB at most");
        }
        if (!looksLike(type, image)) {
            throw badRequest("That isn't a " + type.substring("image/".length()).toUpperCase() + " picture");
        }
        return jdbc.queryForObject("""
                insert into open5e.user_avatars (user_id, content_type, image) values (?, ?, ?)
                on conflict (user_id) do update set content_type = excluded.content_type, image = excluded.image, updated_at = now()
                returning (extract(epoch from updated_at) * 1000)::bigint""", Long.class, userId, type, image);
    }

    public void deleteAvatar(long userId) {
        jdbc.update("delete from open5e.user_avatars where user_id = ?", userId);
    }

    /** The avatar of the user with this username, if they have one. */
    public Optional<Avatar> avatar(String username) {
        return jdbc.query("""
                        select a.content_type, a.image, (extract(epoch from a.updated_at) * 1000)::bigint as version
                        from open5e.user_avatars a join open5e.users u on u.id = a.user_id where u.username = ?""",
                (rs, row) -> new Avatar(rs.getString("content_type"), rs.getBytes("image"), rs.getLong("version")),
                username).stream().findFirst();
    }

    private Long avatarVersion(long userId) {
        return jdbc.query("select (extract(epoch from updated_at) * 1000)::bigint from open5e.user_avatars where user_id = ?",
                rs -> rs.next() ? rs.getLong(1) : null, userId);
    }

    // ----- helpers

    /** The JSON object kept in a table's JSON column for a user, or an empty one. */
    private ObjectNode stored(String sql, long userId) {
        String json = jdbc.query(sql, rs -> rs.next() ? rs.getString(1) : null, userId);
        return json == null ? jsonMapper.createObjectNode() : (ObjectNode) jsonMapper.readTree(json);
    }

    private void upsertJson(String table, String column, long userId, ObjectNode value) {
        jdbc.update("insert into open5e." + table + " (user_id, " + column + ") values (?, ?::jsonb) "
                        + "on conflict (user_id) do update set " + column + " = excluded." + column + ", updated_at = now()",
                userId, jsonMapper.writeValueAsString(value));
    }

    /** "image/PNG; charset=binary" → "image/png" */
    static String baseType(String contentType) {
        if (contentType == null) {
            return "";
        }
        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);
            return (mediaType.getType() + "/" + mediaType.getSubtype()).toLowerCase();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /** Whether the bytes start the way a picture of this type does, so that anything else can't be kept as one. */
    static boolean looksLike(String type, byte[] bytes) {
        return switch (type) {
            case "image/png" -> startsWith(bytes, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/jpeg" -> startsWith(bytes, 0, 0xFF, 0xD8, 0xFF);
            case "image/gif" -> startsWith(bytes, 0, 'G', 'I', 'F', '8') && bytes.length > 5 && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "image/webp" -> startsWith(bytes, 0, 'R', 'I', 'F', 'F') && startsWith(bytes, 8, 'W', 'E', 'B', 'P');
            default -> false;
        };
    }

    private static boolean startsWith(byte[] bytes, int offset, int... expected) {
        if (bytes.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((bytes[offset + i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
