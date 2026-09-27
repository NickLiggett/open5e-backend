package com.main.app.spell;

import com.main.app.common.query.Specs;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.BindParam;

import java.util.List;

/**
 * Query parameters for listing spells. Unset parameters don't filter.
 *
 * @param document only resources in these documents (comma-separated keys)
 * @param name     case-insensitive part of the name
 * @param classKey a class key, e.g. srd-2024_wizard
 */
public record SpellFilter(
        List<String> document,
        String name,
        Integer level,
        String school,
        @BindParam("class") String classKey,
        String damageType,
        Boolean concentration,
        Boolean ritual
) {

    public Specification<Spell> toSpecification() {
        return (root, query, cb) -> Specs.and(cb,
                Specs.inDocuments(root, cb, document),
                Specs.nameContains(root, cb, name),
                Specs.equal(root, cb, "level", level),
                Specs.jsonKey(root, cb, "school", school),
                Specs.jsonArrayContainsKey(root, cb, "classes", classKey),
                Specs.jsonArrayContains(root, cb, "damageTypes", damageType),
                Specs.equal(root, cb, "concentration", concentration),
                Specs.equal(root, cb, "ritual", ritual));
    }
}
