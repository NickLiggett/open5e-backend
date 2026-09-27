package com.main.app.common;

import java.util.List;

public record CrossReferences(List<Link> to) {

    public record Link(String anchor, String url) {
    }
}
