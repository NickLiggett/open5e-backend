package com.main.app.reference;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

/**
 * Query parameters for listing licenses. Unset parameters don't filter.
 *
 * @param name     case-insensitive part of the name
 */
public record LicenseFilter(
        String name
) {

    public Specification<License> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.nameContains(root, cb, name));
    }
}
