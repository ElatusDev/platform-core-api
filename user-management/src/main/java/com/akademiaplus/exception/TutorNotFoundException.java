/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a tutor cannot be found by their identifier.
 */
public class TutorNotFoundException extends RuntimeException {

    /**
     * Constructs a new TutorNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the tutor identifier
     */
    public TutorNotFoundException(String msg) {
        super(msg);
    }
}
