package com.main.app.common.json;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.type.format.jackson.Jackson3JsonFormatMapper;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Makes Hibernate read and write {@code @JdbcTypeCode(SqlTypes.JSON)} fields with {@link DatabaseJson#MAPPER}, so
 * {@code jsonb} columns map straight onto records.
 */
@Configuration(proxyBeanMethods = false)
public class HibernateJsonConfig {

    @Bean
    HibernatePropertiesCustomizer databaseJsonFormatMapper() {
        return properties -> properties.put(MappingSettings.JSON_FORMAT_MAPPER,
                new Jackson3JsonFormatMapper(DatabaseJson.MAPPER));
    }
}
