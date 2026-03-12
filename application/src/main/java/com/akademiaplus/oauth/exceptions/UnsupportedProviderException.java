/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.exceptions;

/**
 * Thrown when the OAuth provider specified in the login request is not supported.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class UnsupportedProviderException extends RuntimeException {

    /** Machine-readable error code. */
    public static final String ERROR_CODE = "UNSUPPORTED_PROVIDER";

    /**
     * Constructs the exception with the unsupported provider name.
     *
     * @param provider the unsupported provider name
     */
    public UnsupportedProviderException(String provider) {
        super("Unsupported OAuth provider: " + provider);
    }
}
