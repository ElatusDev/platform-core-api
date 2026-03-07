/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.exceptions;

/**
 * Thrown when a client exceeds the configured rate limit for an endpoint.
 *
 * @author ElatusDev
 * @since 1.0
 */
public class RateLimitExceededException extends RuntimeException {

    /** Error message for rate limit exceeded. */
    public static final String ERROR_RATE_LIMIT_EXCEEDED = "Rate limit exceeded. Try again in %d seconds.";

    /** Error code for rate limit exceeded responses. */
    public static final String ERROR_CODE = "RATE_LIMIT_EXCEEDED";

    /**
     * Constructs a RateLimitExceededException with a retry-after duration.
     *
     * @param retryAfterSeconds seconds until the client may retry
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        super(String.format(ERROR_RATE_LIMIT_EXCEEDED, retryAfterSeconds));
    }
}
