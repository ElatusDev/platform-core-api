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
 * Spring Security configuration for the Trust Broker (formerly CA service).
 *
 * <p>All endpoints run on a single HTTP port (8082), protected by network
 * isolation ({@code akademia-internal} Docker network / K8s NetworkPolicy).
 * Token validation for {@code /ca/enroll} is handled in the use case layer.
 *
 * <p>CSRF is disabled because the trust broker only accepts machine-to-machine requests.
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
