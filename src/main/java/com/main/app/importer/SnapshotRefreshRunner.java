package com.main.app.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * Refreshes the Open5e snapshot from the live API and exits, when {@code open5e.snapshot.refresh-into} names the folder:
 * <pre>
 * ./gradlew bootRun --args='--spring.profiles.active=import --open5e.snapshot.refresh-into=src/main/resources/open5e-snapshot'
 * </pre>
 * This is the one thing that calls the Open5e API on purpose; imports read the snapshot. It asks for 100 rows at a time
 * with a pause between requests (see {@link Open5eApiSource}), and reports what changed, per table, so that the change to
 * commit can be reviewed. {@code --open5e.snapshot.tables=creatures,spells} refreshes only those, to ask for a little at a
 * time. Add {@code --open5e.import.allow-large-deletions=true} to accept a table with less than half its previous rows.
 */
@Component
@ConditionalOnProperty(name = "open5e.snapshot.refresh-into")
public class SnapshotRefreshRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRefreshRunner.class);

    private final Open5eApiSource api;
    private final DefaultContentImporter importer;
    private final SnapshotWriter writer;
    private final ConfigurableApplicationContext context;
    private final String folder;
    private final String tables;
    private final boolean allowLargeShrink;

    public SnapshotRefreshRunner(Open5eApiSource api, DefaultContentImporter importer, SnapshotWriter writer,
                                 ConfigurableApplicationContext context,
                                 @Value("${open5e.snapshot.refresh-into}") String folder,
                                 @Value("${open5e.snapshot.tables:}") String tables,
                                 @Value("${open5e.import.allow-large-deletions:false}") boolean allowLargeShrink) {
        this.tables = tables.trim();
        this.api = api;
        this.importer = importer;
        this.writer = writer;
        this.context = context;
        this.folder = folder;
        this.allowLargeShrink = allowLargeShrink;
    }

    @Override
    public void run(ApplicationArguments args) {
        int exitCode;
        try {
            List<String> every = importer.tables();
            List<String> chosen = tables.isBlank() ? every : List.of(tables.split("\\s*,\\s*"));
            if (!every.containsAll(chosen)) {
                throw new IllegalArgumentException("open5e.snapshot.tables has " + chosen.stream().filter(t -> !every.contains(t)).toList()
                        + ", which aren't tables of the schema: " + every);
            }
            List<SnapshotWriter.Change> changes = writer.write(api, chosen, Path.of(folder), allowLargeShrink, chosen.size() == every.size());
            log.info("\n{}", format(changes));
            exitCode = 0;
        } catch (RuntimeException e) {
            log.error("Refresh failed; the snapshot was not changed", e);
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }

    static String format(List<SnapshotWriter.Change> changes) {
        StringBuilder text = new StringBuilder(String.format("Open5e snapshot%n%-20s %7s %7s %8s %8s%n", "table", "rows", "added", "changed", "removed"));
        int total = 0;
        for (SnapshotWriter.Change change : changes) {
            text.append(String.format("%-20s %7d %7d %8d %8d%n", change.table(), change.rows(), change.added(), change.changed(), change.removed()));
            total += change.added() + change.changed() + change.removed();
        }
        return text.append(total == 0 ? "No changes: the snapshot was already up to date." : total + " changed rows").toString();
    }
}
