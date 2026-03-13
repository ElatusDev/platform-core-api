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
import com.akademiaplus.task.domain.DomainTask;
import com.akademiaplus.task.interfaceadapters.TaskRepository;
import openapi.akademiaplus.domain.task.service.dto.CreateTaskRequestDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a new task within the current tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class CreateTaskUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationContext applicationContext;
    private final DomainTask domainTask;

    /**
     * Creates a new instance with required dependencies.
     *
     * @param taskRepository     task persistence
     * @param tenantContextHolder tenant context
     * @param applicationContext Spring application context for prototype beans
     * @param domainTask         domain object for task business rules
     */
    public CreateTaskUseCase(TaskRepository taskRepository,
                             TenantContextHolder tenantContextHolder,
                             ApplicationContext applicationContext,
                             DomainTask domainTask) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationContext = applicationContext;
        this.domainTask = domainTask;
    }

    /**
     * Creates a new task within the current tenant after domain validation.
     *
     * @param dto              the creation request DTO
     * @param createdByUserId  the ID of the user creating the task
     * @return the created task DTO
     */
    @Transactional
    public TaskDTO create(CreateTaskRequestDTO dto, Long createdByUserId) {
        TaskDataModel task = applicationContext.getBean(TaskDataModel.class);
        task.setTitle(dto.getTitle());
        task.setDueDate(dto.getDueDate());

        // Domain validates
        domainTask.get(task)
                .validateTitle()
                .validateDueDate();

        task.setDescription(dto.getDescription());
        task.setAssigneeId(dto.getAssigneeId());
        task.setAssigneeType(dto.getAssigneeType().getValue());
        task.setPriority(dto.getPriority().getValue());
        task.setStatus(TaskDataModel.DEFAULT_STATUS);
        task.setCreatedByUserId(createdByUserId);

        TaskDataModel saved = taskRepository.saveAndFlush(task);
        return TaskDtoMapper.toDto(saved);
    }
}
