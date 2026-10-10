package com.main.app.player;

import java.time.Instant;
import java.util.List;

/**
 * A DM's initiative tracker as a member of their party may see it: the turn order (the first is whose turn it is), with
 * the armor class and hit points of monsters and others left out until the DM reveals them.
 *
 * @param dm the DM whose tracker it is
 * @param updatedAt when the DM last saved it
 */
public record PartyTrackerDTO(String dm, Instant updatedAt, List<Combatant> combatants) {

    /**
     * One in the turn order. {@code ac} and {@code hp} are null when they are hidden (or unknown).
     *
     * @param playerId the character, if this row is one (the same id as in {@code /api/players})
     * @param mine whether the character is the viewer's: they own it or play it
     * @param revealed whether the DM has shown this row's armor class and hit points
     */
    public record Combatant(Long id, String name, Integer initiative, String type, Long playerId, boolean mine,
                            Integer ac, Integer hp, boolean revealed) {
    }
}
