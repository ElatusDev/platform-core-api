/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Drives mock data load and cleanup using the FK-safe ordering
 * computed by {@link MockDataExecutionPlan}.
 *
 * <p>The generation flow is tenant-aware:</p>
 * <ol>
 *   <li>Clean all data in reverse-topological order.</li>
 *   <li>Load tenants (committed via {@code REQUIRES_NEW} so FK checks pass).</li>
 *   <li>For each tenant: set {@link TenantContextHolder}, then load all
 *       tenant-scoped entities so Hibernate's {@code TenantPreInsertEventListener}
 *       can assign the correct {@code tenantId}.</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataOrchestrator {

    private static final int DEFAULT_TENANT_COUNT = 1;
    private static final int DEFAULT_ENTITIES_PER_TENANT = 50;

    private final Map<MockEntityType, IntConsumer> mockDataLoaders;
    private final Map<MockEntityType, Runnable> mockDataCleaners;
    private final Map<MockEntityType, MockDataPostLoadHook> mockDataPostLoadHooks;
    private final TenantRepository tenantRepository;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Cleans all mock data, then loads fresh records.
     *
     * <p>Cleanup runs in reverse-topological order (children before parents).
     * Tenant creation commits independently ({@code REQUIRES_NEW}) so that
     * the tenant row is visible when downstream FK checks run.
     * All tenant-scoped entities are loaded per-tenant with the context set.</p>
     *
     * @param tenantCount        number of tenants to create
     * @param entitiesPerTenant  number of records per entity type per tenant
     */
    public void generateAll(int tenantCount, int entitiesPerTenant) {
        MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

        cleanAll(plan);
        loadTenants(tenantCount);
        loadTenantScopedEntities(plan, entitiesPerTenant);

        log.info("Mock data generation complete");
    }

    /**
     * Cleans all mock data, then loads fresh records using default counts.
     */
    public void generateAll() {
        generateAll(DEFAULT_TENANT_COUNT, DEFAULT_ENTITIES_PER_TENANT);
    }

    private void cleanAll(MockDataExecutionPlan plan) {
        log.info("Cleaning mock data in FK-safe order: {}", plan.getCleanupOrder());
        for (MockEntityType entity : plan.getCleanupOrder()) {
            Runnable cleaner = mockDataCleaners.get(entity);
            if (cleaner != null) {
                cleaner.run();
            }
        }
    }

    private void loadTenants(int tenantCount) {
        IntConsumer tenantLoader = mockDataLoaders.get(MockEntityType.TENANT);
        if (tenantLoader != null) {
            log.info("Loading {} tenant(s)", tenantCount);
            tenantLoader.accept(tenantCount);
        }
    }

    private void loadTenantScopedEntities(MockDataExecutionPlan plan, int entitiesPerTenant) {
        List<Long> tenantIds = tenantRepository.findAll().stream()
                .map(TenantDataModel::getTenantId)
                .toList();

        for (Long tenantId : tenantIds) {
            tenantContextHolder.setTenantId(tenantId);
            log.info("Loading {} records per entity type for tenant {}", entitiesPerTenant, tenantId);

            for (MockEntityType entity : plan.getLoadOrder()) {
                if (entity == MockEntityType.TENANT) {
                    continue;
                }

                IntConsumer loader = mockDataLoaders.get(entity);
                if (loader != null) {
                    loader.accept(entitiesPerTenant);
                }

                MockDataPostLoadHook hook = mockDataPostLoadHooks.get(entity);
                if (hook != null) {
                    hook.execute();
                }
            }
        }
    }
}
