/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a course event cannot be found by its identifier.
 */
public class CourseEventNotFoundException extends RuntimeException {

    /**
     * Constructs a new CourseEventNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the course event identifier
     */
    public CourseEventNotFoundException(String msg) {
        super(msg);
    }
}
