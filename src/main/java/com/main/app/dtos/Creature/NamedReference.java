package com.main.app.dtos.Creature;

/**
 * A {@code {"key": ..., "name": ...}} reference, as used by Open5e for publishers, game systems, damage types,
 * conditions and environments.
 */
public record NamedReference(String key, String name) {
}
