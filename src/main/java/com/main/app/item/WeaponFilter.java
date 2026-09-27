package com.main.app.item;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing weapons. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record WeaponFilter(
        List<String> document,
        String name,
        Boolean isSimple,
        Boolean isImprovised
) {

    public Specification<Weapon> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "isSimple", isSimple),
                Specs.equal(root, cb, "isImprovised", isImprovised));
    }
}
