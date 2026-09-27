package com.main.app.importer;

import tools.jackson.databind.node.ObjectNode;

import java.util.List;

/** Where default content comes from: every row of an Open5e v2 endpoint, in the API's JSON shape. */
public interface DefaultContentSource {

    /** @param endpoint an Open5e v2 endpoint, which is also the table name, e.g. {@code spells} */
    List<ObjectNode> fetch(String endpoint);

    /** For messages, e.g. the API's URL. */
    String describe();
}
