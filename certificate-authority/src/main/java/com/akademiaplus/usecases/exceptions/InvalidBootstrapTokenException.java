/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.exceptions;

/**
 * Thrown when a bootstrap enrollment token is not found in the manifest.
 */
public class InvalidBootstrapTokenException extends RuntimeException {

    public InvalidBootstrapTokenException(String message) {
        super(message);
    }
}
