package com.main.app.importer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The content an import loads: everything Open5e has (from the snapshot in this repository, or with
 * {@code open5e.import.source=api} from the live API), plus this repository's own ({@link CustomContent}). The importer
 * can't tell them apart, so custom rows are added, updated and (if removed from their file) deleted along with the rest,
 * and are never deleted just because Open5e doesn't have them.
 * <p>
 * A key both have is an error rather than one overriding the other, so Open5e adding a row with a key you used is noticed.
 */
@Lazy
@Primary
@Component
public class LayeredContentSource implements DefaultContentSource {

    private final DefaultContentSource open5e;
    private final CustomContent custom;

    @Autowired
    public LayeredContentSource(Open5eApiSource api, SnapshotSource snapshot, CustomContent custom,
                                @Value("${open5e.import.source:snapshot}") String which) {
        this(choose(api, snapshot, which), custom);
    }

    private static DefaultContentSource choose(Open5eApiSource api, SnapshotSource snapshot, String which) {
        return switch (which) {
            case "snapshot" -> snapshot;
            case "api" -> api;
            default -> throw new IllegalArgumentException("open5e.import.source must be snapshot or api, not '" + which + "'");
        };
    }

    LayeredContentSource(DefaultContentSource open5e, CustomContent custom) {
        this.open5e = open5e;
        this.custom = custom;
    }

    @Override
    public List<ObjectNode> fetch(String endpoint) {
        List<ObjectNode> rows = new ArrayList<>(open5e.fetch(endpoint));
        List<ObjectNode> own = custom.rows(endpoint);
        if (own.isEmpty()) {
            return rows;
        }
        Set<String> taken = new HashSet<>();
        rows.forEach(row -> taken.add(row.path("key").asString()));
        for (ObjectNode row : own) {
            String key = row.get("key").asString();
            if (taken.contains(key)) {
                throw new IllegalStateException("The custom " + endpoint + " row '" + key + "' has the same key as one from "
                        + open5e.describe() + ". Give the custom one another key");
            }
            rows.add(row);
        }
        return rows;
    }

    @Override
    public String describe() {
        return open5e.describe() + " + custom content (" + custom.size() + " rows)";
    }
}
