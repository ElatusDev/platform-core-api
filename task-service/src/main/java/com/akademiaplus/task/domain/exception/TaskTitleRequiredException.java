/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.task.domain.exception;

/**
 * Thrown when a task is created or updated with a blank or null title.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TaskTitleRequiredException extends RuntimeException {

    /** Error message for missing task title. */
    public static final String ERROR_MESSAGE = "Task title is required";

    /**
     * Creates a new exception with the default error message.
     */
    public TaskTitleRequiredException() {
        super(ERROR_MESSAGE);
    }
}
