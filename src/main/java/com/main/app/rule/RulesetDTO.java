package com.main.app.rule;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;

import java.util.List;

public record RulesetDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        List<NamedReference> rules,
        CrossReferences crossreferences
) {

    public static RulesetDTO from(Ruleset entity) {
        return new RulesetDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getRules(),
                entity.getCrossreferences()
        );
    }
}
