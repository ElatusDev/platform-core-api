/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

public class DatabaseConnectionFailedException extends RuntimeException {
    public DatabaseConnectionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
