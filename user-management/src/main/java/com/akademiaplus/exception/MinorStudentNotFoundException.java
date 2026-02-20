/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a minor student cannot be found by their identifier.
 */
public class MinorStudentNotFoundException extends RuntimeException {

    /**
     * Constructs a new MinorStudentNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the minor student identifier
     */
    public MinorStudentNotFoundException(String msg) {
        super(msg);
    }
}
