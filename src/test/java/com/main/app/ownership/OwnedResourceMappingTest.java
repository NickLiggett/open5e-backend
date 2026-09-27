package com.main.app.ownership;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every entity for a table with a {@code document_key} must extend {@link OwnedResource}; otherwise the visibility
 * filter doesn't apply to it and its rows are visible to everyone.
 */
@SpringBootTest
class OwnedResourceMappingTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void entitiesWithDocumentsExtendOwnedResource() {
        Set<String> tablesWithDocuments = Set.copyOf(jdbc.queryForList("""
                select table_name from information_schema.columns
                where table_schema = 'open5e' and column_name = 'document_key'""", String.class));

        List<String> unprotected = entityManagerFactory.getMetamodel().getEntities().stream()
                .map(EntityType::getJavaType)
                .filter(type -> type.isAnnotationPresent(Table.class))
                .filter(type -> tablesWithDocuments.contains(type.getAnnotation(Table.class).name()))
                .filter(type -> !OwnedResource.class.isAssignableFrom(type))
                .map(Class::getName)
                .collect(Collectors.toList());

        assertTrue(unprotected.isEmpty(), "Entities not covered by the visibility filter: " + unprotected);
    }
}
