package com.main.app.spell;

/**
 * One way to cast a spell: the default, or a higher slot or character level. Fields that don't change from the
 * default are null.
 *
 * @param type {@code default}, {@code ritual}, {@code slot_level_N} or {@code player_level_N}
 */
public record CastingOption(
        String type,
        String desc,
        String damageRoll,
        Integer targetCount,
        String duration,
        String range,
        Boolean concentration,
        Integer shapeSize
) {
}
