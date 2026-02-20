/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Module security configurator for the mock-data endpoint.
 *
 * <p>This configurator is only applied when the {@code dev} or {@code local}
 * profile is active (consumed by the main platform's
 * {@link com.akademiaplus.config.SecurityConfig} filter chain). It permits
 * unauthenticated POST requests to the mock-data generation path so that
 * developers can seed data without a JWT.</p>
 *
 * <p>When running the standalone {@code mock-data-service} profile, the
 * entire filter chain already uses {@code anyRequest().permitAll()}, so
 * this configurator has no effect.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@PropertySource("classpath:mock-data-service.properties")
@Configuration
public class MockDataServiceModuleSecurityConfiguration implements ModuleSecurityConfigurator {
    private final String mockDataServicePathVars;

    public MockDataServiceModuleSecurityConfiguration(@Value("${api.mock.data.service.base-uri}") String mockDataServicePath) {
        String anyPathVar = "/**";
        this.mockDataServicePathVars = mockDataServicePath + anyPathVar;
    }

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth
            .requestMatchers(HttpMethod.POST, mockDataServicePathVars)
            .permitAll();
    }
}