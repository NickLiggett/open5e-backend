package com.main.app.dtos.Creature;

import java.util.List;

public record CreatureLanguages(String asString, List<Language> data) {

    public record Language(String key, String name, String desc) {
    }
}
