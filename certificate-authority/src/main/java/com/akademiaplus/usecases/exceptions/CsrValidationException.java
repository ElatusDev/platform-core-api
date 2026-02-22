/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.exceptions;

/**
 * Thrown when a submitted Certificate Signing Request fails validation.
 *
 * <p>Typical causes: key algorithm is not RSA, key size is below the required minimum,
 * the CSR cannot be parsed, or the CSR subject CN does not match the requested CN.
 */
public class CsrValidationException extends RuntimeException {

    public CsrValidationException(String message) {
        super(message);
    }

    public CsrValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
