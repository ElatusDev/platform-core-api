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
 * Security configuration for the task module.
 * All task endpoints require authentication.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class TaskSecurityConfiguration implements ModuleSecurityConfigurator {

    public static final String TASKS_PATH = "/v1/tasks";
    public static final String TASKS_PATH_ANY = "/v1/tasks/**";

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth
                .requestMatchers(TASKS_PATH).authenticated()
                .requestMatchers(TASKS_PATH_ANY).authenticated();
    }
}
