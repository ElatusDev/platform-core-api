/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.domain.exception;

/**
 * Thrown when attempting to complete a task that is already in COMPLETED status.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TaskAlreadyCompletedException extends RuntimeException {

    /** Error message for already completed task. */
    public static final String ERROR_MESSAGE = "Task is already completed";

    /**
     * Creates a new exception with the offending task ID appended to the message.
     *
     * @param taskId the ID of the task that is already completed
     */
    public TaskAlreadyCompletedException(Long taskId) {
        super(ERROR_MESSAGE + ": " + taskId);
    }
}
