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
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import openapi.akademiaplus.domain.task.service.dto.CreateTaskRequestDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Creates a new task within the current tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CreateTaskUseCase {

    public static final String ERROR_TITLE_REQUIRED = "Task title is required";
    public static final String ERROR_DUE_DATE_IN_PAST = "Due date cannot be in the past";

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationContext applicationContext;

    public CreateTaskUseCase(TaskRepository taskRepository,
                             TenantContextHolder tenantContextHolder,
                             ApplicationContext applicationContext) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationContext = applicationContext;
    }

    @Transactional
    public TaskDTO create(CreateTaskRequestDTO dto, Long createdByUserId) {
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new IllegalArgumentException(ERROR_TITLE_REQUIRED);
        }
        if (dto.getDueDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(ERROR_DUE_DATE_IN_PAST);
        }

        TaskDataModel task = applicationContext.getBean(TaskDataModel.class);
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setAssigneeId(dto.getAssigneeId());
        task.setAssigneeType(dto.getAssigneeType().getValue());
        task.setDueDate(dto.getDueDate());
        task.setPriority(dto.getPriority().getValue());
        task.setStatus(TaskDataModel.DEFAULT_STATUS);
        task.setCreatedByUserId(createdByUserId);

        TaskDataModel saved = taskRepository.saveAndFlush(task);
        return TaskDtoMapper.toDto(saved);
    }
}
