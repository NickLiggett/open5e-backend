package com.main.app.dtos.Creature;

/**
 * A {@code {"key": ..., "name": ...}} reference, as used by Open5e for publishers and game systems.
 */
public record NamedReference(String key, String name) {
}
