package com.main.app.importer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Warns at startup if the database has no default content (e.g. it was started without a dump), which otherwise
 * only shows up as empty lists.
 */
@Component
@Profile("!import")
public class DefaultContentCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultContentCheck.class);

    private final JdbcTemplate jdbc;

    public DefaultContentCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer documents = jdbc.queryForObject("select count(*) from open5e.documents where owner_id is null", Integer.class);
        if (documents == null || documents == 0) {
            log.warn("The database has no default content, so lists will be empty. Load it from the Open5e API with: "
                    + "docker compose run --rm app --spring.profiles.active=import --open5e.import.mode=apply "
                    + "(see README, \"Refreshing default content\").");
        }
    }
}
