package com.main.app.player;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** The signed-in user's player characters: the ones they made, and the ones they play. */
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService players;
    private final InventoryService inventory;

    public PlayerController(PlayerService players, InventoryService inventory) {
        this.players = players;
        this.inventory = inventory;
    }

    /** What the character carries: coins and items. */
    @GetMapping("/{id}/inventory")
    public Inventory inventory(@PathVariable long id) {
        return inventory.get(id);
    }

    /** Replaces what the character carries. The owner and the player can; someone seeing it through their party can't. */
    @PutMapping("/{id}/inventory")
    public Inventory replaceInventory(@PathVariable long id, @RequestBody Inventory request) {
        return inventory.replace(id, request);
    }

    @GetMapping
    public List<PlayerDTO> list() {
        return players.list();
    }

    @GetMapping("/{id}")
    public PlayerDTO get(@PathVariable long id) {
        return players.get(id);
    }

    @PostMapping
    public ResponseEntity<PlayerDTO> create(@RequestBody PlayerRequest request) {
        PlayerDTO player = players.create(request);
        return ResponseEntity.created(URI.create("/api/players/" + player.id())).body(player);
    }

    /** Replaces the character. The user who only plays it can change its numbers, not the owner's parts. */
    @PutMapping("/{id}")
    public PlayerDTO update(@PathVariable long id, @RequestBody PlayerRequest request) {
        return players.update(id, request);
    }

    /** What changes during play, the fields given only. Anyone who can see the character can change it, a DM included. */
    @PatchMapping("/{id}/vitals")
    public PlayerDTO updateVitals(@PathVariable long id, @RequestBody Vitals request) {
        return players.updateVitals(id, request);
    }

    /** Owner only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        players.delete(id);
        return ResponseEntity.noContent().build();
    }
}
