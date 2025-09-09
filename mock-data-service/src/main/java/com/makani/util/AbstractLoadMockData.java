package com.makani.util;

import com.makani.utilities.BatchProcessing;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractLoadMockData<D, M, I> {
    private final DataLoader<D> dataLoader;
    private final DataCleanUp<M, I> dataCleanUp;

    protected AbstractLoadMockData(String location,
                                   BatchProcessing<D> batchProcessing,
                                   Class<D> dtoClass,
                                   JpaRepository<M,I> repository,
                                   Class<M> dataModel,
                                   DataLoader<D> dataLoader,
                                   DataCleanUp<M, I> dataCleanUp) {
        this.dataLoader = dataLoader;
        this.dataCleanUp = dataCleanUp;
        this.dataLoader.setLocation(location);
        this.dataLoader.setBatchProcessing(batchProcessing);
        this.dataLoader.setDtoClass(dtoClass);
        this.dataCleanUp.setDataModel(dataModel);
        this.dataCleanUp.setRepository(repository);
    }

    public void load() {
        dataLoader.load();
    }

    public void clean() {
        dataCleanUp.clean();
    }

}
