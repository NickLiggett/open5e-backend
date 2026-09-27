package com.main.app.item;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing magicitems. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record MagicItemFilter(
        List<String> document,
        String name,
        String category,
        String rarity,
        Boolean requiresAttunement
) {

    public Specification<MagicItem> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "categoryKey", category),
                Specs.equal(root, cb, "rarityKey", rarity),
                Specs.equal(root, cb, "requiresAttunement", requiresAttunement));
    }
}
