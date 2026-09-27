package com.main.app.document;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

/**
 * Query parameters for listing documents. Unset parameters don't filter.
 *
 * @param name       case-insensitive part of the name
 * @param publisher  a publisher key, e.g. kobold-press
 * @param gamesystem a game system key, e.g. 5e-2024
 */
public record DocumentFilter(String name, String publisher, String gamesystem) {

    public Specification<Document> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.nameContains(root, cb, name),
                Specs.jsonKey(root, cb, "publisher", publisher),
                Specs.jsonKey(root, cb, "gamesystem", gamesystem));
    }
}
