package com.main.app.ownership;

import com.main.app.common.web.ResourceNotFoundException;
import com.main.app.document.Document;
import com.main.app.document.DocumentAccess;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates, changes, deletes and copies resources of any type, on behalf of the current user. Request bodies use the
 * same JSON as responses; the server-managed fields ({@link ResourceType#MANAGED_FIELDS}) are ignored in them.
 * Permissions come from {@link DocumentAccess}.
 */
@Service
@Transactional
public class ResourceWriter {

    /** Entity fields that bodies never set: the managed ones plus the document key column. */
    private static final String[] NOT_COPIED = {"key", "document", "documentKey", "derivedFrom"};
    private static final int MAX_COPY_SUFFIX = 100;

    private final EntityManager em;
    private final DocumentAccess access;
    private final JsonMapper jsonMapper;

    public ResourceWriter(EntityManager em, DocumentAccess access, JsonMapper jsonMapper) {
        this.em = em;
        this.access = access;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Creates a resource in the body's {@code document} (a key, or a document object as in responses), or in the
     * user's personal homebrew document if there is none. Its key is the document key plus the body's {@code slug},
     * or a slug of its name.
     */
    public <E extends OwnedResource, D extends Record> ResponseEntity<D> create(ResourceType<E, D> type, ObjectNode body) {
        JsonNode documentField = body.get("document");
        Document document = documentField == null || documentField.isNull()
                ? access.personal()
                : access.writable(documentField.isObject() ? documentField.path("key").asString(null) : documentField.asString());

        String slug = Keys.slugify(text(body, "slug") != null ? text(body, "slug") : text(body, "name"));
        if (slug.isEmpty()) {
            throw badRequest("Give the " + type.noun() + " a name or a slug (letters or digits)");
        }
        String key = Keys.resource(document.getKey(), slug);
        if (em.find(type.entity(), key) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A " + type.noun() + " with key '" + key + "' already exists; use another name or slug");
        }

        E entity = type.newEntity();
        apply(type, entity, body);
        entity.setKey(key);
        setDocument(entity, document);
        requireName(type, entity);
        em.persist(entity);
        em.flush();
        return ResponseEntity.created(URI.create(type.path() + "/" + key)).body(type.toDto().apply(entity));
    }

    /** Replaces every field of a resource with the body's: fields the body leaves out are cleared. */
    public <E extends OwnedResource, D extends Record> D replace(ResourceType<E, D> type, String key, ObjectNode body) {
        E existing = writable(type, key);
        E replacement = type.newEntity();
        apply(type, replacement, body);
        replacement.setKey(existing.getKey());
        setDocument(replacement, existing.getDocument());
        replacement.setDerivedFrom(existing.getDerivedFrom());
        requireName(type, replacement);
        E merged = em.merge(replacement);
        em.flush();
        return type.toDto().apply(merged);
    }

    /** Changes only the fields in the body. */
    public <E extends OwnedResource, D extends Record> D update(ResourceType<E, D> type, String key, ObjectNode body) {
        E existing = writable(type, key);
        apply(type, existing, body);
        requireName(type, existing);
        em.flush();
        return type.toDto().apply(existing);
    }

    public <E extends OwnedResource, D extends Record> void delete(ResourceType<E, D> type, String key) {
        em.remove(writable(type, key));
    }

    /**
     * Copies a resource the user can see (typically default content) into one of their documents, to customize it.
     * The copy links back through {@code derivedFrom}; the original is unchanged.
     */
    public <E extends OwnedResource, D extends Record> ResponseEntity<D> copy(ResourceType<E, D> type, String key,
                                                                              CopyRequest request) {
        E source = visible(type, key);
        Document target = request == null || request.document() == null
                ? access.personal()
                : access.writable(request.document());

        E copy = type.newEntity();
        BeanUtils.copyProperties(Hibernate.unproxy(source), copy, NOT_COPIED);
        copy.setKey(freeKey(type, Keys.resource(target.getKey(), Keys.slugOf(source.getKey()))));
        setDocument(copy, target);
        copy.setDerivedFrom(source.getKey());
        em.persist(copy);
        em.flush();
        return ResponseEntity.created(URI.create(type.path() + "/" + copy.getKey())).body(type.toDto().apply(copy));
    }

    private <E extends OwnedResource> E visible(ResourceType<E, ?> type, String key) {
        E entity = em.find(type.entity(), key);
        if (entity == null) {
            throw new ResourceNotFoundException(type.noun(), key);
        }
        return entity;
    }

    private <E extends OwnedResource> E writable(ResourceType<E, ?> type, String key) {
        E entity = visible(type, key);
        access.writable(entity.getDocumentKey());
        return entity;
    }

    /** Sets the body's fields on the entity, rejecting fields the resource doesn't have. */
    private void apply(ResourceType<?, ?> type, Object entity, ObjectNode body) {
        ObjectNode fields = body.deepCopy();
        ResourceType.MANAGED_FIELDS.forEach(fields::remove);
        fields.remove("slug");

        Set<String> writable = type.writableFields();
        Set<String> unknown = fields.propertyNames().stream().filter(name -> !writable.contains(name))
                .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (!unknown.isEmpty()) {
            throw badRequest("Unknown field" + (unknown.size() > 1 ? "s " : " ") + unknown.stream()
                    .map(name -> "'" + name + "'").collect(Collectors.joining(", ")) + " for a " + type.noun());
        }
        try {
            jsonMapper.readerForUpdating(entity).readValue(fields);
        } catch (JacksonException e) {
            String field = e.getPath().stream()
                    .map(reference -> reference.getPropertyName() != null ? reference.getPropertyName() : "[" + reference.getIndex() + "]")
                    .collect(Collectors.joining(".")).replace(".[", "[");
            throw badRequest("Invalid value" + (field.isEmpty() ? "" : " for '" + field + "'") + ": " + e.getOriginalMessage());
        }
    }

    private static void requireName(ResourceType<?, ?> type, Object entity) {
        BeanWrapperImpl bean = new BeanWrapperImpl(entity);
        if (bean.isReadableProperty("name") && (bean.getPropertyValue("name") == null
                || bean.getPropertyValue("name").toString().isBlank())) {
            throw badRequest("A " + type.noun() + " needs a name");
        }
    }

    private static void setDocument(OwnedResource entity, Document document) {
        entity.setDocument(document);
        entity.setDocumentKey(document.getKey());
    }

    /** The key, or the first of key-2, key-3, … that isn't taken. */
    private <E extends OwnedResource> String freeKey(ResourceType<E, ?> type, String key) {
        if (em.find(type.entity(), key) == null) {
            return key;
        }
        for (int suffix = 2; suffix <= MAX_COPY_SUFFIX; suffix++) {
            String candidate = key + "-" + suffix;
            if (em.find(type.entity(), candidate) == null) {
                return candidate;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Too many copies of '" + key + "'");
    }

    private static String text(ObjectNode body, String field) {
        JsonNode value = body.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
