/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions.security;

public class HashingFailureException extends RuntimeException {
    public HashingFailureException(String msg, Exception cause) {
        super(msg, cause);
    }
    public HashingFailureException(String msg) {
        super(msg);
    }
}
