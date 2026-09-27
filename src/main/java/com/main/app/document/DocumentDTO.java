package com.main.app.document;

import com.main.app.common.NamedReference;

import java.time.OffsetDateTime;
import java.util.List;

public record DocumentDTO(
        String key,
        String name,
        String displayName,
        String desc,
        String type,
        String author,
        OffsetDateTime publicationDate,
        String permalink,
        String distanceUnit,
        String weightUnit,
        NamedReference publisher,
        NamedReference gamesystem,
        List<NamedReference> licenses
) {

    public static DocumentDTO from(Document entity) {
        return new DocumentDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDisplayName(),
                entity.getDesc(),
                entity.getType(),
                entity.getAuthor(),
                entity.getPublicationDate(),
                entity.getPermalink(),
                entity.getDistanceUnit(),
                entity.getWeightUnit(),
                entity.getPublisher(),
                entity.getGamesystem(),
                entity.getLicenses()
        );
    }
}
