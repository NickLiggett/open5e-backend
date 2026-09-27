package com.main.app.reference;

import com.main.app.common.DocumentSummary;

public record ImageDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String fileUrl,
        String altText,
        String attribution
) {

    public static ImageDTO from(Image entity) {
        return new ImageDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getFileUrl(),
                entity.getAltText(),
                entity.getAttribution()
        );
    }
}
