/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.exceptions;

/**
 * Thrown when a token binding verification fails — the JWT fingerprint
 * does not match the current request's device fingerprint.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class TokenBindingException extends RuntimeException {

    /** Error message for token binding mismatch. */
    public static final String ERROR_TOKEN_BINDING_MISMATCH =
            "Token binding mismatch: request fingerprint does not match token fingerprint";

    /** Machine-readable error code. */
    public static final String ERROR_CODE_TOKEN_BINDING = "TOKEN_BINDING_MISMATCH";

    /**
     * Constructs a TokenBindingException with the given message.
     *
     * @param message the detail message
     */
    public TokenBindingException(String message) {
        super(message);
    }
}
