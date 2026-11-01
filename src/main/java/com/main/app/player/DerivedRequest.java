package com.main.app.player;

/**
 * The numbers the sheet works out itself from what the character wears and its abilities, sent back to be kept so the
 * tracker and the party can read them. A field left out (null) is left as it is.
 *
 * @param armorClass      0 to 40
 * @param initiativeBonus -10 to 30
 */
public record DerivedRequest(Integer armorClass, Integer initiativeBonus) {
}
