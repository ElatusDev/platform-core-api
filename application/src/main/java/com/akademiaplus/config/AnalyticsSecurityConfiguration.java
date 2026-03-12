/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Security configuration for analytics endpoints.
 * All analytics endpoints require authentication.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class AnalyticsSecurityConfiguration implements ModuleSecurityConfigurator {

    public static final String ANALYTICS_PATH_ANY = "/v1/analytics/**";

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth.requestMatchers(ANALYTICS_PATH_ANY).authenticated();
    }
}
