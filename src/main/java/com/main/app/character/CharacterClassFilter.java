package com.main.app.character;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing classes. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 * @param subclass true: only subclasses; false: only base classes
 */
public record CharacterClassFilter(
        List<String> document,
        String name,
        String subclassOf,
        Boolean subclass,
        String casterType
) {

    public Specification<CharacterClass> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "subclassOfKey", subclassOf),
                Specs.present(root, cb, "subclassOfKey", subclass),
                Specs.equal(root, cb, "casterType", casterType));
    }
}
