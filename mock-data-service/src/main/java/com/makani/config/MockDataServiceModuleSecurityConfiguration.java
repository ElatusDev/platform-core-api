/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

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