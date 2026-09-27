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

/**
 * Runs an import from the command line and exits, when {@code open5e.import.mode} is set:
 * <pre>
 * --spring.profiles.active=import --open5e.import.mode=dry-run    # report what would change
 * --spring.profiles.active=import --open5e.import.mode=apply      # change it
 * </pre>
 * Add {@code --open5e.import.allow-large-deletions=true} to delete more than half of a table's rows.
 */
@Component
@ConditionalOnProperty(name = "open5e.import.mode")
public class DefaultContentImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultContentImportRunner.class);

    private final DefaultContentImporter importer;
    private final DefaultContentSource source;
    private final ConfigurableApplicationContext context;
    private final String mode;
    private final boolean allowLargeDeletions;

    public DefaultContentImportRunner(DefaultContentImporter importer, DefaultContentSource source,
                                      ConfigurableApplicationContext context,
                                      @Value("${open5e.import.mode}") String mode,
                                      @Value("${open5e.import.allow-large-deletions:false}") boolean allowLargeDeletions) {
        this.importer = importer;
        this.source = source;
        this.context = context;
        this.mode = mode;
        this.allowLargeDeletions = allowLargeDeletions;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("dry-run") && !mode.equals("apply")) {
            throw new IllegalArgumentException("open5e.import.mode must be dry-run or apply, not '" + mode + "'");
        }
        int exitCode;
        try {
            ImportReport report = importer.run(source, mode.equals("apply"), allowLargeDeletions);
            log.info("\n{}", report.format());
            exitCode = 0;
        } catch (RuntimeException e) {
            log.error("Import failed; nothing was changed", e);
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
