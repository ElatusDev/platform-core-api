/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.function.Function;

public abstract class AbstractLoadMockData<D, M, I> {
    private final DataLoader<D, M, I> dataLoader;
    private final DataCleanUp<M, I> dataCleanUp;
    protected AbstractLoadMockData(String location,
                                   Function<D,M> transformer,
                                   Class<D> dtoClass,
                                   JpaRepository<M,I> repository,
                                   Class<M> dataModel,
                                   DataLoader<D, M, I> dataLoader,
                                   DataCleanUp<M, I> dataCleanUp) {
        this.dataLoader = dataLoader;
        this.dataCleanUp = dataCleanUp;
        this.dataLoader.setLocation(location);
        this.dataLoader.setRepository(repository);
        this.dataLoader.setTransformer(transformer);
        this.dataLoader.setDtoClass(dtoClass);
        this.dataCleanUp.setDataModel(dataModel);
        this.dataCleanUp.setRepository(repository);
    }

    public void load(int count) {
        dataLoader.load(count);
    }

    public void clean() {
        dataCleanUp.clean();
    }

}
