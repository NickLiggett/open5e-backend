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
 * @param classKeys class keys (comma-separated), e.g. srd-2024_wizard: spells on the list of any of them
 */
public record SpellFilter(
        List<String> document,
        String name,
        Integer level,
        String school,
        @BindParam("class") List<String> classKeys,
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
                Specs.jsonArrayContainsAnyKey(root, cb, "classes", classKeys),
                Specs.jsonArrayContains(root, cb, "damageTypes", damageType),
                Specs.equal(root, cb, "concentration", concentration),
                Specs.equal(root, cb, "ritual", ritual));
    }
}
