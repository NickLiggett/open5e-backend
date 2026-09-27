package com.main.app.item;

/** The armor profile of an item or magic item. */
public record ArmorStats(
        String key,
        String name,
        String category,
        String acDisplay,
        Integer acBase,
        Boolean acAddDexmod,
        Integer acCapDexmod,
        Boolean grantsStealthDisadvantage,
        Integer strengthScoreRequired
) {
}
