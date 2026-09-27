package com.main.app.creature;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A value per ability. Used for scores, modifiers and saving throws; for {@code savingThrows} only the proficient
 * abilities are present, so absent ones are left out of the JSON rather than sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AbilityScores(
        Integer strength,
        Integer dexterity,
        Integer constitution,
        Integer intelligence,
        Integer wisdom,
        Integer charisma
) {
}
