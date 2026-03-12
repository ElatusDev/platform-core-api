/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.TaskId;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Retrieves a task by ID within the current tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetTaskByIdUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;

    public GetTaskByIdUseCase(TaskRepository taskRepository,
                              TenantContextHolder tenantContextHolder) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Transactional(readOnly = true)
    public TaskDTO get(Long taskId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        TaskDataModel task = taskRepository.findById(new TaskId(tenantId, taskId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TASK, String.valueOf(taskId)));
        return TaskDtoMapper.toDto(task);
    }

    @Transactional(readOnly = true)
    public TaskDataModel getEntity(Long taskId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        return taskRepository.findById(new TaskId(tenantId, taskId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TASK, String.valueOf(taskId)));
    }
}
