/*
 * Copyright (c) 2025 ElatusDev
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
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Scope("prototype")
public class DataCleanUp<M, I> {
    private static final String ANNOTATION_MISSING = "missing table annotation";

    @PersistenceContext
    @NonNull private EntityManager entityManager;

    @Setter
    private Class<M> dataModel;

    @Setter
    private JpaRepository<@NonNull M, @NonNull I> repository;

    @Transactional
    public void clean() {
        repository.deleteAllInBatch();
        String sql = "ALTER TABLE " + getTableName(dataModel) + " AUTO_INCREMENT = 1";
        entityManager.createNativeQuery(sql).executeUpdate();
    }

    private String getTableName(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            return tableAnnotation.name();
        }

        throw new UnprocessableDataModelException(ANNOTATION_MISSING);
    }
}
