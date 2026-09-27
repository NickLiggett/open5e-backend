package com.main.app.rule;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

public record RuleDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        Integer index,
        Integer initialHeaderLevel,
        String ruleset,
        CrossReferences crossreferences
) {

    public static RuleDTO from(Rule entity) {
        return new RuleDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getIndex(),
                entity.getInitialHeaderLevel(),
                entity.getRuleset(),
                entity.getCrossreferences()
        );
    }
}
