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
import com.akademiaplus.task.domain.DomainTask;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Marks a task as completed.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CompleteTaskUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;
    private final DomainTask domainTask;

    /**
     * Creates a new instance with required dependencies.
     *
     * @param taskRepository     task persistence
     * @param tenantContextHolder tenant context
     * @param domainTask         domain object for task business rules
     */
    public CompleteTaskUseCase(TaskRepository taskRepository,
                               TenantContextHolder tenantContextHolder,
                               DomainTask domainTask) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.domainTask = domainTask;
    }

    /**
     * Marks a task as completed after domain validation.
     *
     * @param taskId the ID of the task to complete
     * @return response DTO with taskId and completedAt
     */
    @Transactional
    public CompleteTaskResponseDTO complete(Long taskId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        TaskDataModel task = taskRepository.findById(new TaskId(tenantId, taskId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TASK, String.valueOf(taskId)));

        // Domain validates + produces DTO
        CompleteTaskResponseDTO response = domainTask.get(task).complete();

        // Persist state change
        task.setStatus(DomainTask.COMPLETED_STATUS);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.saveAndFlush(task);

        return response;
    }
}
