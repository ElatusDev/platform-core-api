/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.SQLDelete;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that every {@code @SQLDelete} annotation includes all {@code @Id}
 * columns in its WHERE clause. Prevents the silent data corruption bug where
 * a missing entity ID column causes tenant-wide soft-deletion.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("@SQLDelete WHERE Clause Validation")
class SQLDeleteValidationTest {

    private static final String BASE_PACKAGE = "com.akademiaplus";

    @Test
    @DisplayName("Should include all @Id columns in @SQLDelete WHERE clause")
    void shouldIncludeAllIdColumns_whenSQLDeleteDeclared() {
        // Given
        Reflections reflections = new Reflections(BASE_PACKAGE);
        Set<Class<?>> entityClasses = reflections.getTypesAnnotatedWith(Entity.class);
        List<String> violations = new ArrayList<>();

        // When
        for (Class<?> entityClass : entityClasses) {
            SQLDelete sqlDelete = entityClass.getAnnotation(SQLDelete.class);
            if (sqlDelete == null) {
                continue;
            }

            String sql = sqlDelete.sql().toLowerCase();
            List<String> idColumns = collectIdColumns(entityClass);

            for (String column : idColumns) {
                if (!sql.contains(column.toLowerCase())) {
                    violations.add(entityClass.getSimpleName()
                            + ": @SQLDelete missing @Id column '" + column
                            + "' in WHERE clause. SQL: " + sqlDelete.sql());
                }
            }
        }

        // Then
        assertThat(violations)
                .as("All @SQLDelete annotations must include all @Id columns")
                .isEmpty();
    }

    /**
     * Walks the class hierarchy collecting all {@code @Id} field column names.
     *
     * @param clazz the entity class to inspect
     * @return list of column names for all {@code @Id} fields
     */
    private List<String> collectIdColumns(Class<?> clazz) {
        List<String> columns = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    Column col = field.getAnnotation(Column.class);
                    String colName = (col != null && !col.name().isEmpty())
                            ? col.name()
                            : field.getName();
                    columns.add(colName);
                }
            }
            current = current.getSuperclass();
        }
        return columns;
    }
}
