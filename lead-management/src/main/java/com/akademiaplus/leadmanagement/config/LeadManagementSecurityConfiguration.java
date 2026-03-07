/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.config;

import com.akademiaplus.config.ModuleSecurityConfigurator;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Security configuration for the lead-management module.
 * <p>
 * Permits unauthenticated POST requests to the demo-requests endpoint
 * (public website form submission). All other endpoints require authentication.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class LeadManagementSecurityConfiguration implements ModuleSecurityConfigurator {

    /** Base URL for demo request endpoints. */
    public static final String DEMO_REQUESTS_PATH = "/v1/lead-management/demo-requests";

    /** Base URL with wildcard for path variables. */
    public static final String DEMO_REQUESTS_PATH_ANY = "/v1/lead-management/demo-requests/**";

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth
                .requestMatchers(HttpMethod.POST, DEMO_REQUESTS_PATH).permitAll()
                .requestMatchers(HttpMethod.GET, DEMO_REQUESTS_PATH).authenticated()
                .requestMatchers(HttpMethod.GET, DEMO_REQUESTS_PATH_ANY).authenticated()
                .requestMatchers(HttpMethod.DELETE, DEMO_REQUESTS_PATH_ANY).authenticated();
    }
}
