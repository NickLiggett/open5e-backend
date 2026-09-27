package com.main.app.item;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

/**
 * Query parameters for listing itemrarities. Unset parameters don't filter.
 *
 * @param name     case-insensitive part of the name
 */
public record ItemRarityFilter(
        String name
) {

    public Specification<ItemRarity> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.nameContains(root, cb, name));
    }
}
