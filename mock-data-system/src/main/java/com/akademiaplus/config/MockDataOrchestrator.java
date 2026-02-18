/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Drives mock data load and cleanup using the FK-safe ordering
 * computed by {@link MockDataExecutionPlan}.
 *
 * <p>Instead of hard-coding entity ordering, the orchestrator
 * iterates the topologically sorted plan and looks up each
 * entity's loader, cleaner, and post-load hook from the
 * registry maps produced by {@link MockDataRegistry}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MockDataOrchestrator {

    private static final int DEFAULT_COUNT = 50;

    private final Map<MockEntityType, IntConsumer> mockDataLoaders;
    private final Map<MockEntityType, Runnable> mockDataCleaners;
    private final Map<MockEntityType, MockDataPostLoadHook> mockDataPostLoadHooks;

    /**
     * Cleans all mock data, then loads fresh records.
     *
     * <p>Cleanup runs in reverse-topological order (children before parents)
     * and load runs in topological order (parents before children).
     * Post-load hooks fire after each entity's load completes,
     * enabling inter-entity wiring (e.g., tutor ID injection).</p>
     *
     * @param count number of records to generate per entity type
     */
    public void generateAll(int count) {
        MockDataExecutionPlan plan = MockDataExecutionPlan.forAll();

        log.info("Cleaning mock data in FK-safe order: {}", plan.getCleanupOrder());
        for (MockEntityType entity : plan.getCleanupOrder()) {
            Runnable cleaner = mockDataCleaners.get(entity);
            if (cleaner != null) {
                cleaner.run();
            }
        }

        log.info("Loading {} records per entity type in FK-safe order: {}", count, plan.getLoadOrder());
        for (MockEntityType entity : plan.getLoadOrder()) {
            IntConsumer loader = mockDataLoaders.get(entity);
            if (loader != null) {
                loader.accept(count);
            }

            MockDataPostLoadHook hook = mockDataPostLoadHooks.get(entity);
            if (hook != null) {
                hook.execute();
            }
        }

        log.info("Mock data generation complete");
    }

    /**
     * Cleans all mock data, then loads fresh records using the default count.
     */
    public void generateAll() {
        generateAll(DEFAULT_COUNT);
    }
}
