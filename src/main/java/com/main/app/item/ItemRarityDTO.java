package com.main.app.item;


public record ItemRarityDTO(
        String key,
        String name,
        Integer rank
) {

    public static ItemRarityDTO from(ItemRarity entity) {
        return new ItemRarityDTO(
                entity.getKey(),
                entity.getName(),
                entity.getRank()
        );
    }
}
