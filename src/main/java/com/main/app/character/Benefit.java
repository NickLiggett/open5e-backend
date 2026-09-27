package com.main.app.character;

import com.main.app.common.CrossReferences;

/**
 * A benefit granted by a background or feat. Feat benefits only have a description.
 *
 * @param type for backgrounds, e.g. {@code ability_score}, {@code skill_proficiency}, {@code equipment}
 */
public record Benefit(String name, String desc, String type, CrossReferences crossreferences) {
}
