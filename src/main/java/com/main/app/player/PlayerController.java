package com.main.app.player;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    public PlayerController(PlayerService players) {
        this.players = players;
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

    /** Owner only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        players.delete(id);
        return ResponseEntity.noContent().build();
    }
}
