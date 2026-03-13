/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.domain.exception;

import java.time.LocalDate;

/**
 * Thrown when a task due date is set to a date in the past.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TaskDueDateInPastException extends RuntimeException {

    /** Error message for due date in the past. */
    public static final String ERROR_MESSAGE = "Due date cannot be in the past";

    /**
     * Creates a new exception with the offending due date appended to the message.
     *
     * @param dueDate the due date that was in the past
     */
    public TaskDueDateInPastException(LocalDate dueDate) {
        super(ERROR_MESSAGE + ": " + dueDate);
    }
}
