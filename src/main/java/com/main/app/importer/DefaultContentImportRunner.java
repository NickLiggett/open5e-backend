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
 * <p>
 * By default the content is Open5e's plus this repository's own default custom content ({@link CustomContent}). With
 * {@code --open5e.import.content=custom} only the custom content is applied, without fetching Open5e at all and without
 * touching any of Open5e's rows: a quick way to load or change your own content.
 * <p>
 * Either way, private content (files in {@code custom-content.private-dir}, which belong to a user) is applied last,
 * each file as that user's own documents; see {@link PrivateContentImporter}. If it can't be (its owner has no account
 * yet, say), the command fails, though what was applied before it stays applied.
 */
@Component
@ConditionalOnProperty(name = "open5e.import.mode")
public class DefaultContentImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultContentImportRunner.class);

    private final DefaultContentImporter importer;
    private final DefaultContentSource source;
    private final CustomContent custom;
    private final PrivateContentImporter privateContent;
    private final ConfigurableApplicationContext context;
    private final String mode;
    private final String content;
    private final boolean allowLargeDeletions;

    public DefaultContentImportRunner(DefaultContentImporter importer, DefaultContentSource source, CustomContent custom,
                                      PrivateContentImporter privateContent, ConfigurableApplicationContext context,
                                      @Value("${open5e.import.mode}") String mode,
                                      @Value("${open5e.import.content:all}") String content,
                                      @Value("${open5e.import.allow-large-deletions:false}") boolean allowLargeDeletions) {
        this.importer = importer;
        this.source = source;
        this.custom = custom;
        this.privateContent = privateContent;
        this.context = context;
        this.mode = mode;
        this.content = content;
        this.allowLargeDeletions = allowLargeDeletions;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!mode.equals("dry-run") && !mode.equals("apply")) {
            throw new IllegalArgumentException("open5e.import.mode must be dry-run or apply, not '" + mode + "'");
        }
        if (!content.equals("all") && !content.equals("custom")) {
            throw new IllegalArgumentException("open5e.import.content must be all or custom, not '" + content + "'");
        }
        int exitCode;
        try {
            ImportReport report = content.equals("custom")
                    ? importer.run(custom.asSource(), custom.tables(), mode.equals("apply"), allowLargeDeletions,
                            custom.documentKeys())
                    : importer.run(source, mode.equals("apply"), allowLargeDeletions);
            log.info("\n{}", report.format());
            for (ImportReport own : privateContent.run(mode.equals("apply"), allowLargeDeletions)) {
                log.info("\n{}", own.format());
            }
            exitCode = 0;
        } catch (RuntimeException e) {
            log.error("Import failed; nothing was changed", e);
            exitCode = 1;
        }
        int code = exitCode;
        System.exit(SpringApplication.exit(context, () -> code));
    }
}
