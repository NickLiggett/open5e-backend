package com.main.app.player;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What a character carries. Whoever owns it or plays it reads and replaces it; a DM who sees it through their party
 * can only read it. Anyone else gets "not found", as for the character itself.
 */
@Service
@Transactional
public class InventoryService {

    static final int MAX_LINES = 200;
    static final int MAX_ATTUNED = 3;
    private static final int MAX_COINS = 9_999_999;
    private static final Set<String> KINDS = Set.of("ITEM", "MAGIC");

    private final PlayerService players;
    private final JdbcTemplate jdbc;

    public InventoryService(PlayerService players, JdbcTemplate jdbc) {
        this.players = players;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Inventory get(long characterId) {
        players.get(characterId); // not found unless it can be seen
        return read(characterId);
    }

    public Inventory replace(long characterId, Inventory request) {
        PlayerDTO player = players.get(characterId);
        if (player.role().equals("PARTY")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can see " + player.name() + " because they're in your party, but not change it");
        }
        if (request == null) {
            throw badRequest("An inventory needs its coins and items");
        }
        int platinum = coins(request.platinum(), "platinum");
        int gold = coins(request.gold(), "gold");
        int silver = coins(request.silver(), "silver");
        int copper = coins(request.copper(), "copper");
        List<Inventory.Line> lines = lines(request.items());

        jdbc.update("""
                update open5e.player_characters set platinum = ?, gold = ?, silver = ?, copper = ?, updated_at = now()
                where id = ?""", platinum, gold, silver, copper, characterId);
        jdbc.update("delete from open5e.player_character_items where character_id = ?", characterId);
        int position = 0;
        for (Inventory.Line line : lines) {
            jdbc.update("""
                    insert into open5e.player_character_items
                        (character_id, position, item_key, kind, name, quantity, equipped, attuned, notes)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                    characterId, position++, line.itemKey(), line.kind(), line.name(), line.quantity(), line.equipped(),
                    line.attuned(), line.notes());
        }
        return read(characterId);
    }

    private Inventory read(long characterId) {
        Inventory coins = jdbc.queryForObject("select platinum, gold, silver, copper from open5e.player_characters where id = ?",
                (rs, row) -> new Inventory(rs.getInt("platinum"), rs.getInt("gold"), rs.getInt("silver"),
                        rs.getInt("copper"), null),
                characterId);
        List<Inventory.Line> items = jdbc.query("""
                select item_key, kind, name, quantity, equipped, attuned, notes
                from open5e.player_character_items where character_id = ? order by position""",
                (rs, row) -> new Inventory.Line(rs.getString("item_key"), rs.getString("kind"), rs.getString("name"),
                        rs.getInt("quantity"), rs.getBoolean("equipped"), rs.getBoolean("attuned"), rs.getString("notes")),
                characterId);
        return new Inventory(coins.platinum(), coins.gold(), coins.silver(), coins.copper(), items);
    }

    private static int coins(Integer value, String field) {
        int coins = value == null ? 0 : value;
        if (coins < 0 || coins > MAX_COINS) {
            throw badRequest(field + " must be 0 to " + MAX_COINS);
        }
        return coins;
    }

    private static List<Inventory.Line> lines(List<Inventory.Line> given) {
        if (given == null) {
            return List.of();
        }
        if (given.size() > MAX_LINES) {
            throw badRequest("An inventory can have up to " + MAX_LINES + " lines");
        }
        List<Inventory.Line> lines = new ArrayList<>();
        int attuned = 0;
        for (Inventory.Line line : given) {
            String name = line == null ? null : text(line.name(), "name", 120);
            if (name == null) {
                throw badRequest("Each line needs a name");
            }
            int quantity = line.quantity() == null ? 1 : line.quantity();
            if (quantity < 1 || quantity > 9999) {
                throw badRequest("The quantity of " + name + " must be 1 to 9999");
            }
            String itemKey = text(line.itemKey(), "itemKey", 120);
            String kind = line.kind() == null ? null : line.kind().strip().toUpperCase(Locale.ROOT);
            if (itemKey == null) {
                kind = null;
            } else if (kind == null || !KINDS.contains(kind)) {
                throw badRequest("kind must be ITEM or MAGIC for " + name);
            }
            boolean isAttuned = Boolean.TRUE.equals(line.attuned());
            attuned += isAttuned ? 1 : 0;
            lines.add(new Inventory.Line(itemKey, kind, name, quantity, Boolean.TRUE.equals(line.equipped()), isAttuned,
                    text(line.notes(), "notes", 500)));
        }
        if (attuned > MAX_ATTUNED) {
            throw badRequest("A character can be attuned to " + MAX_ATTUNED + " items at most");
        }
        return lines;
    }

    private static String text(String value, String field, int max) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.length() > max) {
            throw badRequest(field + " can be up to " + max + " characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
