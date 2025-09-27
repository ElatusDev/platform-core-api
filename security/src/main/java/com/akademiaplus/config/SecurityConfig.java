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

    @Bean
    @Profile({"dev", "local"})
    public SecurityFilterChain securityFilterChain(Set<ModuleSecurityConfigurator> moduleSecurityConfigurators, HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSourceForLogin()))
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/v1/security/login/internal").permitAll();
                    try {
                        for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                            configurator.configure(auth);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply module security configurations", e);
                    }
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
        defaultCorsConfig.setAllowCredentials(false);

        CorsConfiguration loginCorsConfig = new CorsConfiguration();
        loginCorsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
        loginCorsConfig.setAllowedMethods(List.of("POST"));
        loginCorsConfig.setAllowedHeaders(List.of("Content-Type"));
        loginCorsConfig.setAllowCredentials(false);

        source.registerCorsConfiguration("/v1/**", defaultCorsConfig);
        source.registerCorsConfiguration("/v1/security/login/internal", loginCorsConfig);
        return source;
    }

    @Bean
    @Profile({"mock-data-service"}) // Use the profile you are activating
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

    @Bean
    @Profile({"mock-data-service"})
    public CorsConfigurationSource corsConfigurationSourceForMockDataService() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration defaultCorsConfig = new CorsConfiguration();
        defaultCorsConfig.setAllowedOriginPatterns(List.of("http://localhost:*", "https://localhost:*"));
        defaultCorsConfig.setAllowedMethods(List.of("POST"));
        defaultCorsConfig.setAllowedHeaders(List.of("*"));
        defaultCorsConfig.setAllowCredentials(false);

        source.registerCorsConfiguration("/infra/v1/mock-data/generate/**", defaultCorsConfig);
        return source;
    }
}