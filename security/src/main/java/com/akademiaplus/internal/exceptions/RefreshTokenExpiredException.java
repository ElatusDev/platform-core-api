/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.exceptions;

/**
 * Thrown when a refresh token has expired or cannot be found.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class RefreshTokenExpiredException extends RuntimeException {

    /** Default error message. */
    public static final String ERROR_MESSAGE = "Refresh token expired or not found";

    /**
     * Constructs a RefreshTokenExpiredException with the default message.
     */
    public RefreshTokenExpiredException() {
        super(ERROR_MESSAGE);
    }

    /**
     * Constructs a RefreshTokenExpiredException with a custom message.
     *
     * @param message the detail message
     */
    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
