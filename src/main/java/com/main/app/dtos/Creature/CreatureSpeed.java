package com.main.app.dtos.Creature;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Movement speeds. For {@code speed} only the creature's movement types are present, so absent ones are left out of
 * the JSON rather than sent as null.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatureSpeed(
        String unit,
        Integer walk,
        Integer fly,
        Integer swim,
        Integer climb,
        Integer burrow,
        Integer crawl,
        Boolean hover
) {
}
