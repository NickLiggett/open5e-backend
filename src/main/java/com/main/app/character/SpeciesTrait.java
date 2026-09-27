package com.main.app.character;

import com.main.app.common.CrossReferences;

/** @param type {@code SIZE} or {@code SPEED} for those traits, otherwise null */
public record SpeciesTrait(String name, String desc, String type, Integer order, CrossReferences crossreferences) {
}
