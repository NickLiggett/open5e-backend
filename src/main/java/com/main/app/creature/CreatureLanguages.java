package com.main.app.creature;

import java.util.List;

public record CreatureLanguages(String asString, List<Language> data) {

    public record Language(String key, String name, String desc) {
    }
}
