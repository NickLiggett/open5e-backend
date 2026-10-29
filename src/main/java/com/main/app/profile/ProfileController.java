package com.main.app.profile;

import com.main.app.user.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * The signed-in user's own settings, avatar picture, initiative tracker state and saved encounters. Each is the user's alone: there is
 * no way to ask for someone else's here (the avatar is served to everyone by {@link UserAvatarController}).
 */
@RestController
@RequestMapping("/api/me")
public class ProfileController {

    private final CurrentUser currentUser;
    private final ProfileService profile;

    public ProfileController(CurrentUser currentUser, ProfileService profile) {
        this.currentUser = currentUser;
        this.profile = profile;
    }

    /** The settings chosen so far (mode, primary, secondary), and {@code avatarVersion}, null without an avatar. */
    @GetMapping("/settings")
    public ObjectNode settings() {
        return profile.settings(userId());
    }

    /** Replaces the settings. {@code mode} is light, dark or system; the colors are {@code #rrggbb}. */
    @PutMapping("/settings")
    public ObjectNode saveSettings(@RequestBody ObjectNode body) {
        return profile.saveSettings(userId(), body);
    }

    /** What the user left their tracker as: any JSON object they saved, or {@code {}}. */
    @GetMapping("/tracker")
    public ObjectNode tracker() {
        return profile.tracker(userId());
    }

    /** Keeps the tracker state. It is the app's to shape: any JSON object up to 256 KB. */
    @PutMapping("/tracker")
    public ObjectNode saveTracker(@RequestBody ObjectNode body) {
        return profile.saveTracker(userId(), body);
    }

    /** The trackers put aside, most recent first: {@code [{id, name, updatedAt, combatants}]}. */
    @GetMapping("/trackers")
    public List<ProfileService.SavedTracker> savedTrackers() {
        return profile.savedTrackers(userId());
    }

    /** Starts a new tracker named {@code name}, putting the one in use aside. Resolves to the tracker now in use. */
    @PostMapping("/trackers")
    public ObjectNode newTracker(@RequestBody ObjectNode body) {
        return profile.newTracker(userId(), body.path("name").isString() ? body.path("name").asString() : null);
    }

    /** Makes a tracker put aside the one in use, putting the one in use aside. Resolves to the tracker now in use. */
    @PostMapping("/trackers/{id}/open")
    public ObjectNode openTracker(@PathVariable long id) {
        return profile.openTracker(userId(), id);
    }

    @DeleteMapping("/trackers/{id}")
    public ResponseEntity<Void> deleteSavedTracker(@PathVariable long id) {
        profile.deleteSavedTracker(userId(), id);
        return ResponseEntity.noContent().build();
    }

    /** The encounters the user saved on the Encounters page: any JSON object they saved, or {@code {}}. */
    @GetMapping("/encounters")
    public ObjectNode encounters() {
        return profile.encounters(userId());
    }

    /** Keeps the saved encounters. They are the app's to shape: any JSON object up to 256 KB. */
    @PutMapping("/encounters")
    public ObjectNode saveEncounters(@RequestBody ObjectNode body) {
        return profile.saveEncounters(userId(), body);
    }

    /**
     * Keeps the body, a PNG, JPEG, WebP or GIF picture of up to 512 KB, as the user's avatar. Everyone can then see it
     * at {@code /api/users/{username}/avatar}. Resolves to its version, which changes with each new picture.
     */
    @PutMapping(value = "/avatar", consumes = {"image/png", "image/jpeg", "image/webp", "image/gif"})
    public Map<String, Long> saveAvatar(HttpServletRequest request) throws IOException {
        long userId = userId();
        // Read one byte past the limit rather than all of it, so that an enormous body can't fill the memory.
        byte[] image = request.getInputStream().readNBytes(ProfileService.MAX_AVATAR_BYTES + 1);
        return Map.of(ProfileService.AVATAR_VERSION, profile.saveAvatar(userId, request.getContentType(), image));
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<Void> deleteAvatar() {
        profile.deleteAvatar(userId());
        return ResponseEntity.noContent().build();
    }

    private long userId() {
        return currentUser.id().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to use your settings"));
    }
}
