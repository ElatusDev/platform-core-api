/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.FailToGenerateMockDataException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Function;

/**
 * Data loader for bridge/join tables that are managed via {@code @JoinTable}
 * in parent entities. Uses native SQL inserts instead of JPA repositories
 * to avoid Hibernate dual-mapping conflicts.
 *
 * <p>The tenant ID is obtained from {@link TenantContextHolder} at load time,
 * matching the behavior of the standard {@link DataLoader} where
 * {@code TenantPreInsertEventListener} assigns the tenant ID during JPA save.</p>
 *
 * @param <D> the record type produced by the factory
 */
@Slf4j
@AllArgsConstructor
public class NativeBridgeDataLoader<D> {

    private final EntityManager entityManager;
    private final TenantContextHolder tenantContextHolder;
    private final DataFactory<D> factory;
    private final String insertSql;
    private final Function<D, Object[]> paramExtractor;

    /**
     * Generates {@code count} bridge records and persists them via native SQL.
     *
     * @param count number of records to generate
     */
    @Transactional
    public void load(int count) {
        try {
            Long tenantId = tenantContextHolder.requireTenantId();
            List<D> records = factory.generate(count);
            for (D record : records) {
                Object[] params = paramExtractor.apply(record);
                var query = entityManager.createNativeQuery(insertSql);
                query.setParameter(1, tenantId);
                for (int i = 0; i < params.length; i++) {
                    query.setParameter(i + 2, params[i]);
                }
                query.executeUpdate();
            }
        } catch (Exception e) {
            throw new FailToGenerateMockDataException(e);
        }
    }
}
