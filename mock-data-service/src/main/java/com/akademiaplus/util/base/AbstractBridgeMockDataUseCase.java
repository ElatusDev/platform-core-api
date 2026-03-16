/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

/**
 * Base class for bridge/join table mock data use cases.
 * Uses native SQL instead of JPA repositories to avoid dual-mapping
 * conflicts with {@code @JoinTable} annotations in parent entities.
 *
 * @param <D> the record type produced by the factory
 */
public abstract class AbstractBridgeMockDataUseCase<D> {

    private final NativeBridgeDataLoader<D> dataLoader;
    private final NativeBridgeDataCleanUp dataCleanUp;

    /**
     * Creates a new bridge mock data use case.
     *
     * @param dataLoader the native bridge data loader
     * @param dataCleanUp the native bridge data cleanup
     */
    protected AbstractBridgeMockDataUseCase(NativeBridgeDataLoader<D> dataLoader,
                                             NativeBridgeDataCleanUp dataCleanUp) {
        this.dataLoader = dataLoader;
        this.dataCleanUp = dataCleanUp;
    }

    /**
     * Loads {@code count} bridge records into the database.
     *
     * @param count number of records to generate and persist
     */
    public void load(int count) {
        dataLoader.load(count);
    }

    /**
     * Removes all bridge records from the table.
     */
    public void clean() {
        dataCleanUp.clean();
    }
}
