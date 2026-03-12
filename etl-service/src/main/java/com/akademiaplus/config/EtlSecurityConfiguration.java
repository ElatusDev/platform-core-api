/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.utilities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Module security configurator for ETL migration endpoints.
 *
 * <p>All {@code /v1/etl/migrations/**} endpoints require authentication
 * and are restricted to ADMIN and PRINCIPAL roles.</p>
 */
@PropertySource("classpath:application-etl-service.properties")
@Configuration
public class EtlSecurityConfiguration implements ModuleSecurityConfigurator {

    private final String migrationsPath;
    private final String migrationsPathVars;

    public EtlSecurityConfiguration(@Value("${api.etl.migrations.base-url}") String migrationsBaseUri) {
        this.migrationsPath = migrationsBaseUri;
        this.migrationsPathVars = migrationsBaseUri + "/**";
    }

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        auth
                .requestMatchers(HttpMethod.POST, migrationsPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.PUT, migrationsPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.DELETE, migrationsPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, migrationsPath, migrationsPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name());
    }
}
