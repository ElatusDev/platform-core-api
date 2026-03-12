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
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Marks a task as completed.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CompleteTaskUseCase {

    public static final String COMPLETED_STATUS = "COMPLETED";
    public static final String ERROR_TASK_ALREADY_COMPLETED = "Task is already completed";

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;

    public CompleteTaskUseCase(TaskRepository taskRepository,
                               TenantContextHolder tenantContextHolder) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Transactional
    public CompleteTaskResponseDTO complete(Long taskId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        TaskDataModel task = taskRepository.findById(new TaskId(tenantId, taskId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TASK, String.valueOf(taskId)));

        if (COMPLETED_STATUS.equals(task.getStatus())) {
            throw new IllegalStateException(ERROR_TASK_ALREADY_COMPLETED);
        }

        LocalDateTime now = LocalDateTime.now();
        task.setStatus(COMPLETED_STATUS);
        task.setCompletedAt(now);
        taskRepository.saveAndFlush(task);

        CompleteTaskResponseDTO response = new CompleteTaskResponseDTO();
        response.setTaskId(taskId);
        response.setCompletedAt(now.atOffset(ZoneOffset.UTC));
        return response;
    }
}
