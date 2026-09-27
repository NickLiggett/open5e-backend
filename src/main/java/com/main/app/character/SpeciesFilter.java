package com.main.app.character;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing species. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record SpeciesFilter(
        List<String> document,
        String name,
        String subspeciesOf,
        Boolean isSubspecies
) {

    public Specification<Species> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "subspeciesOfKey", subspeciesOf),
                Specs.equal(root, cb, "isSubspecies", isSubspecies));
    }
}
