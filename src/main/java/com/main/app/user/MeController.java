package com.main.app.user;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CurrentUser currentUser;

    public MeController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    /** The signed-in user, or 401 if nobody is signed in. */
    @GetMapping
    public ResponseEntity<User> me() {
        return currentUser.id()
                .map(id -> ResponseEntity.ok(new User(id, currentUser.username().orElseThrow())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }
}
