/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for rate limiting.
 * Binds to {@code rate-limit.*} in application.properties.
 *
 * @param enabled whether rate limiting is active
 * @param tiers   per-tier configuration (login, public, authenticated)
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Map<String, TierProperties> tiers
) {

    /**
     * Rate limit tier configuration for a specific endpoint pattern.
     *
     * @param limit     maximum number of requests allowed in the window
     * @param windowMs  sliding window duration in milliseconds
     */
    public record TierProperties(int limit, long windowMs) {}
}
