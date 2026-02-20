/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.exception;

/**
 * Exception thrown when a compensation cannot be found by its identifier.
 */
public class CompensationNotFoundException extends RuntimeException {

    /**
     * Constructs a new CompensationNotFoundException with the specified detail message.
     *
     * @param msg the detail message containing the compensation identifier
     */
    public CompensationNotFoundException(String msg) {
        super(msg);
    }
}
