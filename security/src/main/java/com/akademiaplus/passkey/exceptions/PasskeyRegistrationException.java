/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.exceptions;

/**
 * Exception thrown when passkey registration fails.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class PasskeyRegistrationException extends RuntimeException {

    /** Error message format for registration failure. */
    public static final String ERROR_REGISTRATION_FAILED = "Passkey registration failed: %s";

    /** Error message when a credential with the same ID already exists. */
    public static final String ERROR_CREDENTIAL_ALREADY_EXISTS = "A passkey with this credential ID already exists";

    /**
     * Constructs with a message.
     *
     * @param message the error message
     */
    public PasskeyRegistrationException(String message) {
        super(message);
    }

    /**
     * Constructs with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public PasskeyRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
