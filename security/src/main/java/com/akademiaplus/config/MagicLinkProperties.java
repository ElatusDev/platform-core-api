/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for magic link authentication.
 *
 * @param baseUrl                    the base URL for magic link verification (e.g. frontend URL)
 * @param tokenExpiryMinutes         how long the token remains valid
 * @param maxRequestsPerEmailPerHour rate limit per email address per hour
 * @param emailSubject               email subject line for the magic link email
 * @author ElatusDev
 * @since 1.0
 */
@ConfigurationProperties(prefix = "magic-link")
public record MagicLinkProperties(
        String baseUrl,
        int tokenExpiryMinutes,
        int maxRequestsPerEmailPerHour,
        String emailSubject
) {
}
