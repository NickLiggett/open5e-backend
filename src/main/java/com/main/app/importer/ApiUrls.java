package com.main.app.importer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.util.regex.Pattern;

/**
 * Open5e data links to other resources with URLs on whichever Open5e server produced it, e.g.
 * {@code https://api.open5e.com/v2/spells/srd_fireball/}. The endpoints here are named the same, so those links
 * become paths on this API: {@code /api/spells/srd_fireball}. V6 did the same to the data already in the database.
 */
public final class ApiUrls {

    private static final Pattern OPEN5E_LINK = Pattern.compile("^https?://[^/]+/v2/([a-z]+)/([^/?#]+)/?(\\?.*)?$");

    private ApiUrls() {
    }

    /** The path on this API for an Open5e API link, or the value unchanged if it isn't one. */
    public static String rewrite(String value) {
        return OPEN5E_LINK.matcher(value).replaceFirst("/api/$1/$2");
    }

    /** A copy with every string that's an Open5e API link rewritten. */
    public static JsonNode rewrite(JsonNode node) {
        if (node.isString()) {
            return JsonNodeFactory.instance.stringNode(rewrite(node.asString()));
        }
        if (node.isObject()) {
            ObjectNode copy = JsonNodeFactory.instance.objectNode();
            node.properties().forEach(e -> copy.set(e.getKey(), rewrite(e.getValue())));
            return copy;
        }
        if (node.isArray()) {
            ArrayNode copy = JsonNodeFactory.instance.arrayNode();
            node.forEach(child -> copy.add(rewrite(child)));
            return copy;
        }
        return node;
    }
}
