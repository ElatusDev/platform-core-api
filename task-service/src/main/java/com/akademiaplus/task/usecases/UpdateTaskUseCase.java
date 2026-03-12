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
import openapi.akademiaplus.domain.task.service.dto.UpdateTaskRequestDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Updates an existing task, applying only non-null fields.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class UpdateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;

    public UpdateTaskUseCase(TaskRepository taskRepository,
                             TenantContextHolder tenantContextHolder) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Transactional
    public TaskDTO update(Long taskId, UpdateTaskRequestDTO dto) {
        Long tenantId = tenantContextHolder.requireTenantId();
        TaskDataModel task = taskRepository.findById(new TaskId(tenantId, taskId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.TASK, String.valueOf(taskId)));

        if (dto.getTitle() != null) {
            task.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            task.setDescription(dto.getDescription());
        }
        if (dto.getAssigneeId() != null) {
            task.setAssigneeId(dto.getAssigneeId());
        }
        if (dto.getAssigneeType() != null) {
            task.setAssigneeType(dto.getAssigneeType().getValue());
        }
        if (dto.getDueDate() != null) {
            task.setDueDate(dto.getDueDate());
        }
        if (dto.getPriority() != null) {
            task.setPriority(dto.getPriority().getValue());
        }
        if (dto.getStatus() != null) {
            task.setStatus(dto.getStatus().getValue());
        }

        TaskDataModel saved = taskRepository.saveAndFlush(task);
        return TaskDtoMapper.toDto(saved);
    }
}
