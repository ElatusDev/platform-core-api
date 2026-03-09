/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.UnprocessableDataModelException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.regex.Pattern;

/**
 * Cleanup utility for platform-level entities that use {@link JpaRepository}
 * instead of {@code TenantScopedRepository}.
 *
 * <p>Deletes all rows and resets the auto-increment counter for the entity table.</p>
 *
 * @param <M> the JPA entity type
 */
@RequiredArgsConstructor
public class PlatformDataCleanUp<M> {

    private static final String ANNOTATION_MISSING = "missing table annotation";
    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @PersistenceContext
    @NonNull private EntityManager entityManager;

    @Setter
    private Class<M> dataModel;

    @Setter
    private JpaRepository<M, Long> repository;

    /**
     * Deletes all rows in the entity table and resets the auto-increment counter.
     */
    @SuppressWarnings("java:S2077") // Table name sourced from @Table annotation, validated by regex
    @Transactional
    public void clean() {
        repository.deleteAllInBatch();
        String sql = "ALTER TABLE " + getTableName(dataModel) + " AUTO_INCREMENT = 1";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private String getTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            String name = tableAnnotation.name();
            if (!VALID_TABLE_NAME.matcher(name).matches()) {
                throw new UnprocessableDataModelException("Invalid table name: " + name);
            }
            return name;
        }

        throw new UnprocessableDataModelException(ANNOTATION_MISSING);
    }
}
