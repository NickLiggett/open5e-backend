package com.main.app.character;

import com.main.app.common.CrossReferences;

import java.util.List;

/**
 * A class or subclass feature.
 *
 * @param featureType      e.g. {@code CLASS_LEVEL_FEATURE}, {@code CLASS_TABLE_DATA}
 * @param gainedAt         the levels at which it's gained, with a level-specific detail if any
 * @param dataForClassTable the values of a class-table column by level, for {@code CLASS_TABLE_DATA} features
 */
public record ClassFeature(
        String key,
        String name,
        String desc,
        String featureType,
        List<GainedAt> gainedAt,
        List<ClassTableEntry> dataForClassTable,
        CrossReferences crossreferences
) {

    public record GainedAt(Integer level, String detail) {
    }

    public record ClassTableEntry(Integer level, String columnValue) {
    }
}
