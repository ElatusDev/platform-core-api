/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions.security;

public class EncryptionFailureException extends RuntimeException {
    public EncryptionFailureException(String msg, Exception e) {
        super(msg, e);
    }
    public EncryptionFailureException(String msg) { super(msg); }
}
