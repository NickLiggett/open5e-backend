package com.main.app.rule;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing rulesets. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 */
public record RulesetFilter(
        List<String> document,
        String name
) {

    public Specification<Ruleset> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name));
    }
}
