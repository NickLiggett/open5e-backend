package com.main.app.reference;


public record GameSystemDTO(
        String key,
        String name,
        String desc,
        String contentPrefix
) {

    public static GameSystemDTO from(GameSystem entity) {
        return new GameSystemDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDesc(),
                entity.getContentPrefix()
        );
    }
}
