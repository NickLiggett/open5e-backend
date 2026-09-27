package com.main.app.dtos.Creature;

import java.util.List;

public record CrossReferences(List<Link> to) {

    public record Link(String anchor, String url) {
    }
}
