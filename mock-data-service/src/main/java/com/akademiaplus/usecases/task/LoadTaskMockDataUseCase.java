/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.task;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock task records into the database.
 */
@Service
public class LoadTaskMockDataUseCase
        extends AbstractMockDataUseCase<TaskDataModel, TaskDataModel, TaskId> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the data loader for task records
     * @param dataCleanUp the data cleanup for the tasks table
     */
    public LoadTaskMockDataUseCase(
            DataLoader<TaskDataModel, TaskDataModel, TaskId> dataLoader,
            @Qualifier("taskDataCleanUp")
            DataCleanUp<TaskDataModel, TaskId> dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
