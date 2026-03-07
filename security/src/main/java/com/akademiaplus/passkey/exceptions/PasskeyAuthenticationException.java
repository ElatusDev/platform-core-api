/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.exceptions;

/**
 * Exception thrown when passkey authentication fails.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class PasskeyAuthenticationException extends RuntimeException {

    /** Error message format for authentication failure. */
    public static final String ERROR_AUTHENTICATION_FAILED = "Passkey authentication failed: %s";

    /** Error message when no credential is found for the provided ID. */
    public static final String ERROR_CREDENTIAL_NOT_FOUND = "No passkey credential found for the provided ID";

    /** Error message when sign count regression is detected. */
    public static final String ERROR_SIGN_COUNT_REGRESSION = "Authenticator sign count regression detected — possible cloned authenticator";

    /**
     * Constructs with a message.
     *
     * @param message the error message
     */
    public PasskeyAuthenticationException(String message) {
        super(message);
    }

    /**
     * Constructs with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public PasskeyAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
