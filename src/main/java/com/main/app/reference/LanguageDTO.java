package com.main.app.reference;

import com.main.app.common.CrossReferences;
import com.main.app.common.DocumentSummary;

public record LanguageDTO(
        String key,
        String name,
        DocumentSummary document,
        String derivedFrom,
        String desc,
        Boolean isExotic,
        Boolean isSecret,
        String scriptLanguage,
        CrossReferences crossreferences
) {

    public static LanguageDTO from(Language entity) {
        return new LanguageDTO(
                entity.getKey(),
                entity.getName(),
                entity.getDocument().toSummary(),
                entity.getDerivedFrom(),
                entity.getDesc(),
                entity.getIsExotic(),
                entity.getIsSecret(),
                entity.getScriptLanguage(),
                entity.getCrossreferences()
        );
    }
}
