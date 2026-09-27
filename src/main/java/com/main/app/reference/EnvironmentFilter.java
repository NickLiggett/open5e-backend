package com.main.app.reference;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing environments. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record EnvironmentFilter(
        List<String> document,
        String name,
        Boolean aquatic,
        Boolean planar,
        Boolean interior
) {

    public Specification<Environment> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "aquatic", aquatic),
                Specs.equal(root, cb, "planar", planar),
                Specs.equal(root, cb, "interior", interior));
    }
}
