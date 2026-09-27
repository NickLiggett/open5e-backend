package com.main.app.creature;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing creatures. Unset parameters don't filter.
 *
 * @param document only creatures in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 * @param cr       exact challenge rating, e.g. 0.25
 * @param crMin    minimum challenge rating
 * @param crMax    maximum challenge rating
 * @param type     a creature type key, e.g. dragon
 * @param size     a size key, e.g. large
 */
public record CreatureFilter(
        List<String> document,
        String name,
        Float cr,
        Float crMin,
        Float crMax,
        String type,
        String size
) {

    public Specification<Creature> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "challengeRating", cr),
                Specs.atLeast(root, cb, "challengeRating", crMin),
                Specs.atMost(root, cb, "challengeRating", crMax),
                Specs.jsonKey(root, cb, "type", type),
                Specs.jsonKey(root, cb, "size", size));
    }
}
