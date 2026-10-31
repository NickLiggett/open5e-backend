package com.main.app.player;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Answering and keeping track of requests to play characters. A request is made on a character ({@code POST
 * /api/players/{id}/requests}, in {@code PlayerController}).
 */
@RestController
@RequestMapping("/api/character-requests")
public class CharacterRequestController {

    private final CharacterRequestService requests;

    public CharacterRequestController(CharacterRequestService requests) {
        this.requests = requests;
    }

    /** The requests you've made that are waiting or were turned down. */
    @GetMapping("/sent")
    public List<CharacterRequestDTO> sent() {
        return requests.sent();
    }

    /** Withdraws a request, or dismisses one that was turned down. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        requests.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /** The requests to play a character that are waiting for your answer. */
    @GetMapping("/received")
    public List<IncomingCharacterRequestDTO> received() {
        return requests.received();
    }

    /** Agrees to play the character. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable long id) {
        requests.accept(id);
        return ResponseEntity.noContent().build();
    }

    /** Turns the request down. */
    @PostMapping("/{id}/deny")
    public ResponseEntity<Void> deny(@PathVariable long id) {
        requests.deny(id);
        return ResponseEntity.noContent().build();
    }
}
