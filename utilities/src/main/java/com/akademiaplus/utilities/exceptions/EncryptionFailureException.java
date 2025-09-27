/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

public class EncryptionFailureException extends RuntimeException {
    public EncryptionFailureException(Exception e) {
        super(e);
    }
    public EncryptionFailureException(String msg) { super(msg); }
}
