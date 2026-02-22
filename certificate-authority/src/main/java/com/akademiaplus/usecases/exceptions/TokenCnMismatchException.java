/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.exceptions;

/**
 * Thrown when the Common Name in an enrollment request does not match the CN
 * to which the bootstrap token is bound.
 */
public class TokenCnMismatchException extends RuntimeException {

    public TokenCnMismatchException(String message) {
        super(message);
    }
}
