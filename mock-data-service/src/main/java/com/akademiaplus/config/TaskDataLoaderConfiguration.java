/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Spring configuration for task-related mock data loader and cleanup beans.
 */
@Configuration
public class TaskDataLoaderConfiguration {

    // ── Task ──

    /**
     * Creates the data loader for task records.
     *
     * @param repository  the task repository
     * @param factory     the task data factory
     * @return a configured data loader
     */
    @Bean
    public DataLoader<TaskDataModel, TaskDataModel, TaskId> taskDataLoader(
            TaskRepository repository,
            DataFactory<TaskDataModel> factory) {

        return new DataLoader<>(repository, Function.identity(), factory);
    }

    /**
     * Creates the data cleanup for the tasks table.
     *
     * @param entityManager the JPA entity manager
     * @param repository    the task repository
     * @return a configured data cleanup
     */
    @Bean
    public DataCleanUp<TaskDataModel, TaskId> taskDataCleanUp(
            EntityManager entityManager,
            TaskRepository repository) {

        DataCleanUp<TaskDataModel, TaskId> cleanUp = new DataCleanUp<>(entityManager);
        cleanUp.setDataModel(TaskDataModel.class);
        cleanUp.setRepository(repository);
        return cleanUp;
    }
}
