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
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskListResponseDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskStatusDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lists tasks with optional filters and pagination.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class ListTasksUseCase {

    private final TaskRepository taskRepository;
    private final TenantContextHolder tenantContextHolder;

    public ListTasksUseCase(TaskRepository taskRepository,
                            TenantContextHolder tenantContextHolder) {
        this.taskRepository = taskRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Transactional(readOnly = true)
    public TaskListResponseDTO list(TaskStatusDTO status, Long assigneeId,
                                    TaskPriorityDTO priority, Boolean overdue,
                                    int page, int size) {
        Long tenantId = tenantContextHolder.requireTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<TaskDataModel> result = resolveQuery(tenantId, status, assigneeId,
                priority, overdue, pageable);

        List<TaskDTO> content = result.getContent().stream()
                .map(TaskDtoMapper::toDto)
                .toList();

        TaskListResponseDTO response = new TaskListResponseDTO();
        response.setContent(content);
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPage(page);
        response.setSize(size);
        return response;
    }

    private Page<TaskDataModel> resolveQuery(Long tenantId, TaskStatusDTO status,
                                             Long assigneeId, TaskPriorityDTO priority,
                                             Boolean overdue, Pageable pageable) {
        if (Boolean.TRUE.equals(overdue)) {
            return taskRepository.findOverdueTasks(tenantId, pageable);
        }
        if (status != null) {
            return taskRepository.findAllByTenantIdAndStatus(tenantId, status.getValue(), pageable);
        }
        if (assigneeId != null) {
            return taskRepository.findAllByTenantIdAndAssigneeId(tenantId, assigneeId, pageable);
        }
        if (priority != null) {
            return taskRepository.findAllByTenantIdAndPriority(tenantId, priority.getValue(), pageable);
        }
        return taskRepository.findAllByTenantId(tenantId, pageable);
    }
}
