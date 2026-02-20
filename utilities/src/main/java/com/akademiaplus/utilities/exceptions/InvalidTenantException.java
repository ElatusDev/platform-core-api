/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

/**
 * Thrown when a tenant context is required but not available.
 * <p>
 * Handled by {@link com.akademiaplus.utilities.web.BaseControllerAdvice}
 * → HTTP 400 Bad Request.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class InvalidTenantException extends RuntimeException {

    /** Default message when no tenant context is set. */
    public static final String DEFAULT_MESSAGE = "Tenant context is required";

    /**
     * Creates a new exception with the {@link #DEFAULT_MESSAGE}.
     */
    public InvalidTenantException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Creates a new exception with a custom message.
     *
     * @param msg the detail message
     */
    public InvalidTenantException(String msg) {
        super(msg);
    }
}
