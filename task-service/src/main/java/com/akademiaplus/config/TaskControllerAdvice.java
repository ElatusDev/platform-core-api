/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.task.domain.exception.TaskAlreadyCompletedException;
import com.akademiaplus.task.interfaceadapters.TaskController;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.web.BaseControllerAdvice;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Controller advice for task module exception handling.
 *
 * @author ElatusDev
 * @since 1.0
 */
@ControllerAdvice(basePackageClasses = TaskController.class)
public class TaskControllerAdvice extends BaseControllerAdvice {

    /** Error code for attempting to complete an already-completed task. */
    public static final String CODE_TASK_ALREADY_COMPLETED = "TASK_ALREADY_COMPLETED";

    /**
     * Creates the task controller advice.
     *
     * @param messageService i18n message resolution service
     */
    public TaskControllerAdvice(MessageService messageService) {
        super(messageService);
    }

    /**
     * Handles task already completed exceptions.
     *
     * @param ex the domain exception
     * @return HTTP 409 with error details
     */
    @ExceptionHandler(TaskAlreadyCompletedException.class)
    public ResponseEntity<ErrorResponseDTO> handleTaskAlreadyCompleted(
            TaskAlreadyCompletedException ex) {
        ErrorResponseDTO error = new ErrorResponseDTO(ex.getMessage());
        error.setCode(CODE_TASK_ALREADY_COMPLETED);
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }
}
