package com.main.app.reference;


public record PublisherDTO(
        String key,
        String name
) {

    public static PublisherDTO from(Publisher entity) {
        return new PublisherDTO(
                entity.getKey(),
                entity.getName()
        );
    }
}
