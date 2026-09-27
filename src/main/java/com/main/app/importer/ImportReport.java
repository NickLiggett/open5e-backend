package com.main.app.importer;

import java.util.List;
import java.util.Set;

/**
 * What an import did, or would do (dry run), per table.
 *
 * @param applied false for a dry run: nothing was changed
 */
public record ImportReport(String source, boolean applied, List<Table> tables) {

    /**
     * @param skippedUserKeys upstream rows whose key belongs to a user's content, which is never overwritten
     * @param unknownFields   upstream fields with no matching column, e.g. new in the API; not imported
     */
    public record Table(String name, int fetched, int inserted, int updated, int unchanged, int deleted,
                        Set<String> skippedUserKeys, Set<String> unknownFields) {
    }

    public int changes() {
        return tables.stream().mapToInt(t -> t.inserted() + t.updated() + t.deleted()).sum();
    }

    /** A plain-text table for logs and the console. */
    public String format() {
        StringBuilder out = new StringBuilder();
        out.append(applied ? "Imported" : "Dry run (nothing changed)").append(" from ").append(source).append('\n');
        out.append(String.format("%-17s %8s %9s %8s %10s %8s%n", "table", "fetched", "inserted", "updated", "unchanged", "deleted"));
        for (Table t : tables) {
            out.append(String.format("%-17s %8d %9d %8d %10d %8d%n",
                    t.name(), t.fetched(), t.inserted(), t.updated(), t.unchanged(), t.deleted()));
            if (!t.skippedUserKeys().isEmpty()) {
                out.append("  skipped (keys belong to user content): ").append(t.skippedUserKeys()).append('\n');
            }
            if (!t.unknownFields().isEmpty()) {
                out.append("  not imported (no matching column): ").append(t.unknownFields()).append('\n');
            }
        }
        out.append(changes()).append(" change").append(changes() == 1 ? "" : "s").append('\n');
        return out.toString();
    }
}
