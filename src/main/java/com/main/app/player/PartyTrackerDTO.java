package com.main.app.player;

import java.time.Instant;
import java.util.List;

/**
 * A DM's initiative tracker as a member of their party may see it: the turn order (the first is whose turn it is), with
 * the armor class and hit points of monsters and others left out until the DM reveals them.
 *
 * <p>A DM can have several: the one in use ({@code live}) and any they have put aside, each its own entry.
 *
 * @param dm the DM whose tracker it is
 * @param trackerId null for the one in use, otherwise the id of the tracker put aside
 * @param name what the DM called it, or "" if they didn't
 * @param live whether it is the DM's tracker in use now
 * @param updatedAt when the DM last saved it
 */
public record PartyTrackerDTO(String dm, Long trackerId, String name, boolean live, Instant updatedAt, List<Combatant> combatants) {

    /**
     * One in the turn order. {@code name}, {@code ac} and {@code hp} are null when they are hidden (or unknown).
     *
     * @param playerId the character, if this row is one (the same id as in {@code /api/players})
     * @param mine whether the character is the viewer's: they own it or play it
     * @param revealed whether the DM has shown all of what can be hidden of this row: its armor class and hit points (false for characters)
     * @param conditions the official conditions the row has, in the order the DM gave them; none if it has none. They are
     *                   shown for every row, a monster's included: a condition is something the table can see happen
     */
    public record Combatant(Long id, String name, Integer initiative, String type, Long playerId, boolean mine,
                            Integer ac, Integer hp, boolean revealed, List<String> conditions) {
    }
}
