package com.main.app.importer;

import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** One lot of custom content: its rows by Open5e endpoint, and the keys of the documents they belong to. */
public final class ContentSet {

    private final Map<String, List<ObjectNode>> rows;
    private final Set<String> documentKeys;

    ContentSet(Map<String, List<ObjectNode>> rows, Set<String> documentKeys) {
        this.rows = rows;
        this.documentKeys = documentKeys;
    }

    /** The rows for an Open5e endpoint, none if there are none. Each call gives copies. */
    public List<ObjectNode> rows(String endpoint) {
        return rows.getOrDefault(endpoint, List.of()).stream().map(ObjectNode::deepCopy).toList();
    }

    /** The endpoints (tables) there are rows for. */
    public List<String> tables() {
        return List.copyOf(rows.keySet());
    }

    /** The keys of the documents defined here. */
    public Set<String> documentKeys() {
        return documentKeys;
    }

    public int size() {
        return rows.values().stream().mapToInt(List::size).sum();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** Just this content, as an import source. */
    public DefaultContentSource asSource(String label) {
        return new DefaultContentSource() {
            @Override
            public List<ObjectNode> fetch(String endpoint) {
                return rows(endpoint);
            }

            @Override
            public String describe() {
                return label + " (" + size() + " rows in " + rows.size() + " tables)";
            }
        };
    }
}
