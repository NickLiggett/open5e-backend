package com.main.app.player;

import java.util.List;

/**
 * What a character carries, as sent and as answered.
 *
 * @param platinum platinum pieces, 0 if left out
 * @param gold     gold pieces, 0 if left out
 * @param silver   silver pieces, 0 if left out
 * @param copper   copper pieces, 0 if left out
 * @param items    the lines, in order; none if left out
 */
public record Inventory(Integer platinum, Integer gold, Integer silver, Integer copper, List<Line> items) {

    /**
     * One line of the list.
     *
     * @param itemKey  the key of the Item or Magic item it was picked as, or null for a line typed by hand
     * @param kind     {@code ITEM} or {@code MAGIC} with an {@code itemKey}, else null
     * @param name     what is shown, always given
     * @param quantity 1 to 9999, 1 if left out
     * @param attuned  at most three lines can be attuned
     */
    public record Line(String itemKey, String kind, String name, Integer quantity, Boolean equipped, Boolean attuned,
                       String notes) {
    }
}
