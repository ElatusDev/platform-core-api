/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.domain;

import com.akademiaplus.task.TaskDataModel;
import com.akademiaplus.task.domain.exception.TaskAlreadyCompletedException;
import com.akademiaplus.task.domain.exception.TaskDueDateInPastException;
import com.akademiaplus.task.domain.exception.TaskTitleRequiredException;
import openapi.akademiaplus.domain.task.service.dto.CompleteTaskResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Encapsulates business rules for the Task entity.
 *
 * <p>Validates task invariants (title, due date, state transitions) and
 * produces DTOs for state-changing operations. Has zero I/O — all data
 * is received via {@link #get(TaskDataModel)}.</p>
 *
 * @see TaskDataModel
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class DomainTask {

    /** Completed status value. */
    public static final String COMPLETED_STATUS = "COMPLETED";

    private TaskDataModel dataModel;

    /**
     * Entry point — sets the data model for subsequent operations.
     *
     * @param task the task data model to operate on
     * @return this instance for fluent chaining
     */
    public DomainTask get(TaskDataModel task) {
        this.dataModel = task;
        return this;
    }

    /**
     * Validates that the task title is not null or blank.
     *
     * @return this instance for fluent chaining
     * @throws TaskTitleRequiredException if title is null or blank
     */
    public DomainTask validateTitle() {
        if (dataModel.getTitle() == null || dataModel.getTitle().isBlank()) {
            throw new TaskTitleRequiredException();
        }
        return this;
    }

    /**
     * Validates that the task due date is not in the past.
     *
     * @return this instance for fluent chaining
     * @throws TaskDueDateInPastException if due date is before today
     */
    public DomainTask validateDueDate() {
        if (dataModel.getDueDate().isBefore(LocalDate.now())) {
            throw new TaskDueDateInPastException(dataModel.getDueDate());
        }
        return this;
    }

    /**
     * Completes the task — validates it is not already completed,
     * then produces a response DTO with the completion timestamp.
     *
     * <p>Does NOT mutate the data model — the caller (use case) is responsible
     * for setting status and completedAt on the DataModel before persisting.</p>
     *
     * @return response DTO with taskId and completedAt
     * @throws TaskAlreadyCompletedException if task is already completed
     */
    public CompleteTaskResponseDTO complete() {
        if (COMPLETED_STATUS.equals(dataModel.getStatus())) {
            throw new TaskAlreadyCompletedException(dataModel.getTaskId());
        }

        LocalDateTime now = LocalDateTime.now();
        CompleteTaskResponseDTO response = new CompleteTaskResponseDTO();
        response.setTaskId(dataModel.getTaskId());
        response.setCompletedAt(now.atOffset(ZoneOffset.UTC));
        return response;
    }
}
