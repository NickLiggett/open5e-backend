package com.main.app.item;

/**
 * A property on a particular weapon, e.g. Versatile with detail "1d10".
 *
 * @param detail the weapon-specific value, if any
 */
public record WeaponPropertyUse(String detail, Property property) {

    /** @param type {@code Mastery} for mastery properties, otherwise null */
    public record Property(String name, String desc, String type) {
    }
}
