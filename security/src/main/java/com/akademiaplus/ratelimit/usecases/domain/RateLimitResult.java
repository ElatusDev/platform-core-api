/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.ratelimit.usecases.domain;

/**
 * Result of a rate limit check. Contains the decision and metadata
 * for response headers.
 *
 * @param allowed            whether the request is within the rate limit
 * @param limit              the maximum number of requests in the window
 * @param remaining          remaining requests in the current window
 * @param resetEpochSeconds  epoch second when the window resets
 * @author ElatusDev
 * @since 1.0
 */
public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetEpochSeconds
) {}
