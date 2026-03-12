/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.exceptions;

/**
 * Thrown when the OAuth provider rejects the authorization code exchange
 * or returns an unexpected error.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class OAuthProviderException extends RuntimeException {

    /** Machine-readable error code. */
    public static final String ERROR_CODE = "OAUTH_PROVIDER_ERROR";

    /**
     * Constructs the exception with the provider name.
     *
     * @param provider the OAuth provider that failed
     */
    public OAuthProviderException(String provider) {
        super("Failed to exchange authorization code with " + provider);
    }

    /**
     * Constructs the exception with the provider name and root cause.
     *
     * @param provider the OAuth provider that failed
     * @param cause    the root cause
     */
    public OAuthProviderException(String provider, Throwable cause) {
        super("Failed to exchange authorization code with " + provider, cause);
    }
}
