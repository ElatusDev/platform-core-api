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
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.regex.Pattern;

/**
 * Cleanup utility for bridge/join tables that are managed via {@code @JoinTable}
 * in parent entities. Uses native SQL DELETE instead of JPA repositories.
 *
 * <p>Bridge tables have no auto-increment columns, so no counter reset is needed.</p>
 */
@RequiredArgsConstructor
public class NativeBridgeDataCleanUp {

    static final String INVALID_TABLE_NAME_PREFIX = "Invalid table name: ";
    private static final Pattern VALID_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @PersistenceContext
    @NonNull private final EntityManager entityManager;

    @Setter
    private String tableName;

    /**
     * Deletes all rows from the bridge table via native SQL.
     */
    @SuppressWarnings("java:S2077") // Table name validated by regex
    @Transactional
    public void clean() {
        if (tableName == null || !VALID_TABLE_NAME.matcher(tableName).matches()) {
            throw new UnprocessableDataModelException(INVALID_TABLE_NAME_PREFIX + tableName);
        }
        entityManager.createNativeQuery("DELETE FROM " + tableName).executeUpdate();
    }
}
