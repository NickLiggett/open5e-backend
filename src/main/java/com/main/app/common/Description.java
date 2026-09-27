package com.main.app.common;

/**
 * A description of a rules concept (ability, condition, damage type, …) from one source. Concepts described by
 * several sources have one entry per source.
 *
 * @param document   the key of the source document
 * @param gamesystem the key of its game system
 */
public record Description(String desc, String document, String gamesystem) {
}
