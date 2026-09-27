package com.main.app.creature;

import com.main.app.common.NamedReference;

import java.util.List;

/**
 * Damage and condition lists reference damage types and conditions by key. The display strings are the source text,
 * which can include qualifiers the lists don't capture (e.g. "bludgeoning from nonmagical attacks").
 */
public record ResistancesAndImmunities(
        List<NamedReference> damageImmunities,
        String damageImmunitiesDisplay,
        List<NamedReference> damageResistances,
        String damageResistancesDisplay,
        List<NamedReference> damageVulnerabilities,
        String damageVulnerabilitiesDisplay,
        List<NamedReference> conditionImmunities,
        String conditionImmunitiesDisplay
) {
}
