package com.main.app.player;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

/**
 * The current user's party, and the parties they've been asked to join. A user joins only by accepting, and the DM
 * then sees the player characters that user owns or plays (see {@code GET /api/players/party}).
 */
@RestController
@RequestMapping("/api/party")
public class PartyController {

    private final PartyService party;
    private final PlayerService players;
    private final PartyTrackerService trackers;

    public PartyController(PartyService party, PlayerService players, PartyTrackerService trackers) {
        this.party = party;
        this.players = players;
        this.trackers = trackers;
    }

    /** The people in your party, and those you've asked who haven't answered. */
    @GetMapping
    public List<PartyMemberDTO> members() {
        return party.members();
    }

    /** Asks a user to join your party. */
    @PutMapping("/members/{username}")
    public PartyMemberDTO invite(@PathVariable String username) {
        return party.invite(username);
    }

    /** Removes someone from your party, or withdraws the request. */
    @DeleteMapping("/members/{username}")
    public ResponseEntity<Void> remove(@PathVariable String username) {
        party.remove(username);
        return ResponseEntity.noContent().build();
    }

    /** The parties you've been asked to join, and those you're in. */
    @GetMapping("/invitations")
    public List<PartyInvitationDTO> invitations() {
        return party.invitations();
    }

    /** Joins the DM's party. */
    @PostMapping("/invitations/{dm}/accept")
    public PartyInvitationDTO accept(@PathVariable String dm) {
        return party.accept(dm);
    }

    /** Turns down a request, or leaves the party. */
    @DeleteMapping("/invitations/{dm}")
    public ResponseEntity<Void> leave(@PathVariable String dm) {
        party.leave(dm);
        return ResponseEntity.noContent().build();
    }

    /** The player characters of the people in your party, which you can see but not change. */
    @GetMapping("/players")
    public List<PlayerDTO> players() {
        return players.party();
    }

    /**
     * The initiative trackers of the DMs whose party you are in and whose fight one of your characters is in, as a
     * player sees them: monsters' armor class and hit points hidden until the DM reveals them. It is meant to be asked
     * again every few seconds, so it answers 304 while nothing has changed.
     */
    @GetMapping("/trackers")
    public ResponseEntity<List<PartyTrackerDTO>> trackers(WebRequest request) {
        List<PartyTrackerDTO> shown = trackers.trackers();
        String eTag = "\"" + Integer.toHexString(shown.hashCode()) + "\"";
        if (request.checkNotModified(eTag)) {
            return null; // already answered: 304
        }
        return ResponseEntity.ok().eTag(eTag).cacheControl(CacheControl.noCache()).body(shown);
    }
}
