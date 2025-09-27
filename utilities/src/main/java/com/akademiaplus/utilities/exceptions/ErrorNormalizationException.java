/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

public class ErrorNormalizationException extends RuntimeException {
    public ErrorNormalizationException(Exception e) {
        super(e);
    }

    public ErrorNormalizationException(String msg) {
        super(msg);
    }
}
