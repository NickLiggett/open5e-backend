package com.main.app.common;

/**
 * A {@code {"key": ..., "name": ...}} reference, as used by Open5e for publishers, game systems, damage types,
 * conditions and environments.
 */
public record NamedReference(String key, String name) {
}
