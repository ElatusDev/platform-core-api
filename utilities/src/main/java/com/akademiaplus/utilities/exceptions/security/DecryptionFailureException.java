/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions.security;

public class DecryptionFailureException extends RuntimeException {
    public DecryptionFailureException(Exception e) {
        super(e);
    }
    public DecryptionFailureException(String msg, Exception e) {
        super(msg, e);
    }
    public DecryptionFailureException(String msg) {
        super(msg);
    }
}
