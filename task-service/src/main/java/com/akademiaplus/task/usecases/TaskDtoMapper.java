/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.usecases;

import com.akademiaplus.task.TaskDataModel;
import openapi.akademiaplus.domain.task.service.dto.AssigneeTypeDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskPriorityDTO;
import openapi.akademiaplus.domain.task.service.dto.TaskStatusDTO;

import java.time.ZoneOffset;

/**
 * Maps between {@link TaskDataModel} and {@link TaskDTO}.
 *
 * @author ElatusDev
 * @since 1.0
 */
final class TaskDtoMapper {

    private TaskDtoMapper() {
    }

    static TaskDTO toDto(TaskDataModel entity) {
        TaskDTO dto = new TaskDTO();
        dto.setId(entity.getTaskId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setAssigneeId(entity.getAssigneeId());
        dto.setAssigneeType(AssigneeTypeDTO.fromValue(entity.getAssigneeType()));
        dto.setDueDate(entity.getDueDate());
        dto.setPriority(TaskPriorityDTO.fromValue(entity.getPriority()));
        dto.setStatus(TaskStatusDTO.fromValue(entity.getStatus()));
        dto.setCreatedBy(entity.getCreatedByUserId());
        if (entity.getCreatedAt() != null) {
            dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getCompletedAt() != null) {
            dto.setCompletedAt(entity.getCompletedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }
}
