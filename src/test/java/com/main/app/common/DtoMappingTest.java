package com.main.app.common;

import com.main.app.ownership.OwnedResource;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For every entity {@code X}, checks {@code XDTO.from(X)} against every row of default content: each DTO component
 * must equal the entity field of the same name (catching swapped or mistyped mappings), and every entity field must
 * be in the DTO unless it's listed here as internal (catching columns left out of the API).
 */
@SpringBootTest
@Transactional(readOnly = true)
class DtoMappingTest {

    /** Entity fields deliberately left out of DTOs. */
    private static final Set<String> INTERNAL_FIELDS = Set.of(
            "documentKey",   // duplicated by document.key
            "categoryKey",   // duplicated by category.key
            "rarityKey",     // duplicated by rarity.key
            "subclassOfKey"  // duplicated by subclassOf.key
    );

    @Autowired
    private EntityManager em;

    @Test
    void everyDtoMatchesItsEntity() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Class<?> entity : Entities.all(em)) {
            Class<?> dto = Class.forName(entity.getName() + "DTO");
            Method from = dto.getMethod("from", entity);
            RecordComponent[] components = dto.getRecordComponents();
            Set<String> componentNames = java.util.Arrays.stream(components).map(RecordComponent::getName)
                    .collect(Collectors.toSet());

            for (Field field : Entities.fields(entity)) {
                String name = field.getName();
                if (!componentNames.contains(name) && !INTERNAL_FIELDS.contains(name)) {
                    problems.add(entity.getSimpleName() + "." + name + " is not in " + dto.getSimpleName());
                }
            }

            for (Object loaded : em.createQuery("select e from " + entity.getSimpleName() + " e").getResultList()) {
                Object row = Hibernate.unproxy(loaded); // may already be a lazy proxy from an earlier entity's document
                Object mapped = from.invoke(null, row);
                for (RecordComponent component : components) {
                    Object actual = component.getAccessor().invoke(mapped);
                    Object expected;
                    if (component.getName().equals("document") && row instanceof OwnedResource owned) {
                        expected = owned.getDocument().toSummary();
                    } else {
                        Field field = Entities.field(entity, component.getName()).orElse(null);
                        if (field == null) {
                            problems.add(dto.getSimpleName() + "." + component.getName() + " has no entity field");
                            continue;
                        }
                        expected = Entities.get(field, row);
                    }
                    if (!Objects.equals(expected, actual)) {
                        problems.add(dto.getSimpleName() + "." + component.getName() + " differs for "
                                + Entities.get(Entities.field(entity, "key").orElseThrow(), row));
                    }
                }
            }
        }

        List<String> distinct = problems.stream().distinct().toList();
        assertTrue(distinct.isEmpty(), distinct.size() + " problems:\n"
                + String.join("\n", distinct.subList(0, Math.min(20, distinct.size()))));
    }
}
