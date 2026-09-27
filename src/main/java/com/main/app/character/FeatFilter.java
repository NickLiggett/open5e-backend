package com.main.app.character;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing feats. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record FeatFilter(
        List<String> document,
        String name,
        Boolean hasPrerequisite,
        String type
) {

    public Specification<Feat> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "hasPrerequisite", hasPrerequisite),
                Specs.equal(root, cb, "type", type));
    }
}
