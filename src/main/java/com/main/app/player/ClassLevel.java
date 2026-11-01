package com.main.app.player;

/**
 * One class of a character and the levels taken in it.
 *
 * @param classKey  the key of an Open5e class, if one was picked
 * @param className what's shown, always given
 * @param level     the levels in this class, 1 to 20
 * @param subclassKey  the key of an Open5e subclass, if one was picked
 * @param subclassName the subclass chosen, if any (shown whether or not it has a key)
 */
public record ClassLevel(String classKey, String className, int level, String subclassKey, String subclassName) {
}
