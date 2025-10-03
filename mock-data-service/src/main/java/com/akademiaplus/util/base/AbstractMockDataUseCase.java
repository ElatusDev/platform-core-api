/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

public abstract class AbstractMockDataUseCase<D, M, I> {
    private final DataLoader<D, M, I> dataLoader;
    private final DataCleanUp<M, I> dataCleanUp;

    protected AbstractMockDataUseCase(DataLoader<D, M, I> dataLoader,
                                      DataCleanUp<M, I> dataCleanUp) {
        this.dataLoader = dataLoader;
        this.dataCleanUp = dataCleanUp;
    }

    public void load(int count) {
        dataLoader.load(count);
    }

    public void clean() {
        dataCleanUp.clean();
    }

}
