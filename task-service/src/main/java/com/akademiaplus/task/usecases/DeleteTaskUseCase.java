/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Soft-deletes a task by composite key.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteTaskUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteTaskUseCase(TaskRepository taskRepository,
                             TenantContextHolder tenantContextHolder) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Transactional
    public void delete(Long taskId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                taskRepository,
                new TaskId(tenantId, taskId),
                EntityType.TASK,
                String.valueOf(taskId));
    }
}
