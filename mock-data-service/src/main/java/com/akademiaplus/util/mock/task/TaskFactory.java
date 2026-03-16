/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.task;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Factory for creating {@link TaskDataModel} instances with fake data.
 *
 * <p>Requires employee IDs to be injected via setter before
 * {@link #generate(int)} is called. Employee IDs are used for both
 * the assignee and the task creator.</p>
 */
@Component
@SuppressWarnings("java:S2245") // Random used for non-security test data generation
public class TaskFactory implements DataFactory<TaskDataModel> {

    /** Error message when employee IDs have not been set. */
    public static final String ERROR_EMPLOYEE_IDS_NOT_SET =
            "availableEmployeeIds must be set before generating tasks";

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final int MAX_COMPLETED_DAYS_AGO = 14;

    private final ApplicationContext applicationContext;
    private final TaskDataGenerator generator;
    private final Random random;

    @Setter
    private List<Long> availableEmployeeIds = List.of();

    /**
     * Constructs the factory with the application context and data generator.
     *
     * @param applicationContext the Spring application context for prototype bean creation
     * @param generator          the task data generator
     */
    public TaskFactory(ApplicationContext applicationContext,
                       TaskDataGenerator generator) {
        this.applicationContext = applicationContext;
        this.generator = generator;
        this.random = new Random();
    }

    /**
     * Generates the specified number of {@link TaskDataModel} instances.
     *
     * @param count the number of tasks to generate
     * @return a list of generated task data models
     * @throws IllegalStateException if employee IDs have not been set
     */
    @Override
    public List<TaskDataModel> generate(int count) {
        if (availableEmployeeIds.isEmpty()) {
            throw new IllegalStateException(ERROR_EMPLOYEE_IDS_NOT_SET);
        }

        List<TaskDataModel> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tasks.add(createTask(i));
        }
        return tasks;
    }

    private TaskDataModel createTask(int index) {
        TaskDataModel model = applicationContext.getBean(TaskDataModel.class);

        Long assigneeId = availableEmployeeIds.get(index % availableEmployeeIds.size());
        Long creatorId = availableEmployeeIds.get(
                (index + 1) % availableEmployeeIds.size()
        );

        String status = generator.status();

        model.setTitle(generator.title());
        model.setDescription(generator.description());
        model.setAssigneeId(assigneeId);
        model.setAssigneeType(generator.assigneeType());
        model.setDueDate(generator.dueDate());
        model.setPriority(generator.priority());
        model.setStatus(status);
        model.setCreatedByUserId(creatorId);
        model.setCompletedAt(resolveCompletedAt(status));
        return model;
    }

    private LocalDateTime resolveCompletedAt(String status) {
        if (STATUS_COMPLETED.equals(status)) {
            int daysAgo = random.nextInt(MAX_COMPLETED_DAYS_AGO) + 1;
            return LocalDateTime.now().minusDays(daysAgo);
        }
        return null;
    }
}
