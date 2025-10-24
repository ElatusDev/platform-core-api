/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions.security;

import com.google.i18n.phonenumbers.NumberParseException;

public class ErrorNormalizationException extends RuntimeException {
    public ErrorNormalizationException(Exception e) {
        super(e);
    }

    public ErrorNormalizationException(String msg) {
        super(msg);
    }

    public ErrorNormalizationException(String msg, NumberParseException e) {
        super(msg, e);
    }
}
