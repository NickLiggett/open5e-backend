package com.main.app.reference;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

/**
 * Query parameters for listing gamesystems. Unset parameters don't filter.
 *
 * @param name     case-insensitive part of the name
 */
public record GameSystemFilter(
        String name
) {

    public Specification<GameSystem> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.nameContains(root, cb, name));
    }
}
