package com.main.app.profile;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * Users' avatar pictures, for showing next to their name. Anyone can ask for one by username: a browser's {@code <img>}
 * can't send a token, and a picture people choose to show is not secret. There is no way to list users from here.
 */
@RestController
@RequestMapping("/api/users")
public class UserAvatarController {

    private final ProfileService profile;

    public UserAvatarController(ProfileService profile) {
        this.profile = profile;
    }

    /**
     * The picture, or 404 if the user has none (or there is no such user). It can be kept but must be checked each
     * time: the answer is 304 while the picture is unchanged.
     */
    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable String username, WebRequest request) {
        ProfileService.Avatar avatar = profile.avatar(username.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No avatar for '" + username + "'"));
        String eTag = "\"" + avatar.version() + "\"";
        if (request.checkNotModified(eTag)) {
            return null; // already answered: 304
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                .eTag(eTag)
                .cacheControl(CacheControl.noCache())
                .body(avatar.image());
    }
}
