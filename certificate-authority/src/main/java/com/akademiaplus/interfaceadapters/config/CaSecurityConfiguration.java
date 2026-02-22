/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the CA service.
 *
 * <p>All application-layer authentication is handled at the transport layer:
 * <ul>
 *   <li>Port 8081 — Tomcat enforces mTLS ({@code client-auth=required}); no Spring Security needed</li>
 *   <li>Port 8082 — one-way TLS; {@code /ca/enroll} token validation is done in the use case</li>
 *   <li>Port 8082 — {@code /ca/ca.crt} is fully public (serves the root cert for bootstrapping)</li>
 * </ul>
 *
 * <p>CSRF is disabled because the CA service only accepts machine-to-machine requests.
 */
@Configuration
@EnableWebSecurity
public class CaSecurityConfiguration {

    @Bean
    public SecurityFilterChain caSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(CsrfConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
