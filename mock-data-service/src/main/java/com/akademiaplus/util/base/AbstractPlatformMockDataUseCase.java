/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

/**
 * Abstract base for mock data use cases targeting platform-level entities.
 *
 * <p>Platform-level entities use {@link PlatformDataLoader} and
 * {@link PlatformDataCleanUp} backed by {@code JpaRepository}
 * instead of {@code TenantScopedRepository}.</p>
 *
 * @param <D> the DTO or data model type produced by the factory
 * @param <M> the JPA entity type
 */
public abstract class AbstractPlatformMockDataUseCase<D, M> {

    private final PlatformDataLoader<D, M> dataLoader;
    private final PlatformDataCleanUp<M> dataCleanUp;

    protected AbstractPlatformMockDataUseCase(PlatformDataLoader<D, M> dataLoader,
                                               PlatformDataCleanUp<M> dataCleanUp) {
        this.dataLoader = dataLoader;
        this.dataCleanUp = dataCleanUp;
    }

    /**
     * Generates and persists {@code count} mock records.
     *
     * @param count number of records to generate
     */
    public void load(int count) {
        dataLoader.load(count);
    }

    /**
     * Deletes all records and resets the auto-increment counter.
     */
    public void clean() {
        dataCleanUp.clean();
    }
}
