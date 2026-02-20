/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a course cannot be found by its identifier.
 */
public class CourseNotFoundException extends RuntimeException {

    /**
     * Constructs a new CourseNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the course identifier
     */
    public CourseNotFoundException(String msg) {
        super(msg);
    }
}
