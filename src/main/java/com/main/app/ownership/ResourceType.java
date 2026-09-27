package com.main.app.ownership;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Describes a writable resource for {@link ResourceWriter}: its entity and DTO, where it lives in the API, and what
 * to call it in messages.
 *
 * @param path e.g. {@code /api/spells}
 * @param noun e.g. {@code spell}
 */
public record ResourceType<E extends OwnedResource, D extends Record>(
        Class<E> entity,
        Class<D> dto,
        String path,
        String noun,
        Function<E, D> toDto
) {

    /** Fields the server sets; ignored in request bodies, so a fetched resource can be sent back as it is. */
    public static final Set<String> MANAGED_FIELDS = Set.of("key", "document", "derivedFrom");

    /** Fields a request body may set: the DTO's fields except the managed ones. */
    public Set<String> writableFields() {
        return Arrays.stream(dto.getRecordComponents())
                .map(RecordComponent::getName)
                .filter(name -> !MANAGED_FIELDS.contains(name))
                .collect(Collectors.toSet());
    }

    public E newEntity() {
        try {
            return entity.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Can't create " + entity.getName(), e);
        }
    }
}
