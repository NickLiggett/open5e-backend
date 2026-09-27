package com.main.app.reference;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * Query parameters for listing alignments. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 */
public record AlignmentFilter(
        List<String> document
) {

    public Specification<Alignment> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document));
    }
}
