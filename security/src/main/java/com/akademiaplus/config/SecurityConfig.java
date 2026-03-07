/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.internal.interfaceadapters.jwt.JwtRequestFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @SuppressWarnings("java:S4502") // CSRF disabled: stateless JWT Bearer auth, no cookies/sessions
    @Bean
    @Profile({"dev", "local"})
    public SecurityFilterChain securityFilterChain(Set<ModuleSecurityConfigurator> moduleSecurityConfigurators, HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSourceForLogin()))
                .headers(headers -> headers
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true))
                        .contentTypeOptions(contentType -> {})
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
                )
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/actuator/**").permitAll()
                            .requestMatchers("/v1/security/login/internal").permitAll()
                            .requestMatchers("/v1/security/register").permitAll()
                            .requestMatchers("/v1/security/token/refresh").permitAll()
                            .requestMatchers("/v1/security/logout").permitAll()
                            .requestMatchers("/v3/api-docs/**").permitAll()
                            .requestMatchers("/swagger-ui/**").permitAll()
                            .requestMatchers("/swagger-ui.html").permitAll();
                    try {
                        for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                            configurator.configure(auth);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply module security configurations", e);
                    }
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Profile({"dev", "local"})
    public CorsConfigurationSource corsConfigurationSourceForLogin() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration defaultCorsConfig = new CorsConfiguration();
        defaultCorsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
        defaultCorsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        defaultCorsConfig.setAllowedHeaders(List.of("*"));
        defaultCorsConfig.setAllowCredentials(true);

        CorsConfiguration loginCorsConfig = new CorsConfiguration();
        loginCorsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
        loginCorsConfig.setAllowedMethods(List.of("POST"));
        loginCorsConfig.setAllowedHeaders(List.of("Content-Type"));
        loginCorsConfig.setAllowCredentials(true);

        source.registerCorsConfiguration("/v1/**", defaultCorsConfig);
        source.registerCorsConfiguration("/v1/security/login/internal", loginCorsConfig);
        return source;
    }

    /**
     * Security filter chain for the internal mock-data service.
     *
     * <p>This service is internal infrastructure invoked only by CI pipelines
     * (e.g. GitHub Actions) or local developer scripts. It requires no
     * authentication, no JWT, and no {@code X-Tenant-Id} header. The
     * {@link com.akademiaplus.infra.persistence.config.TenantContextLoader}
     * filter is disabled separately via
     * {@code MockDataTenantConfiguration}.</p>
     */
    @SuppressWarnings("java:S4502") // CSRF disabled: internal mock-data-service, stateless API
    @Bean
    @Profile({"mock-data-service"})
    public SecurityFilterChain securityFilterChainForMockDataService(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSourceForMockDataService()))
            .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }

    /**
     * CORS configuration for the internal mock-data service.
     *
     * <p>Allows POST from localhost (local dev) and any origin (CI runners).
     * This service is never exposed to the public internet.</p>
     */
    @Bean
    @Profile({"mock-data-service"})
    public CorsConfigurationSource corsConfigurationSourceForMockDataService() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        corsConfig.setAllowedMethods(List.of("POST"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(false);

        source.registerCorsConfiguration("/**", corsConfig);
        return source;
    }
}