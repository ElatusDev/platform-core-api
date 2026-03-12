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
 * Security configuration for self-service endpoints.
 * Requires CUSTOMER role for all {@code /v1/my/**} paths.
 *
 * <p>Internal users (employees, collaborators) do not have the CUSTOMER
 * role and will receive 403 Forbidden when accessing these endpoints.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class MyEndpointsSecurityConfiguration implements ModuleSecurityConfigurator {

    /** Path pattern for all self-service endpoints. */
    public static final String MY_PATH_ANY = "/v1/my/**";

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth.requestMatchers(MY_PATH_ANY).hasRole("CUSTOMER");
    }
}
