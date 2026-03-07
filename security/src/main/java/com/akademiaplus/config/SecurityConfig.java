/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext;
import com.akademiaplus.internal.interfaceadapters.filters.AppOriginFilter;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtRequestFilter;
import com.akademiaplus.ratelimit.interfaceadapters.RateLimitingFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Security configuration with dual filter chains for akademia-plus-web
 * and elatusdev-web applications.
 *
 * <p>Requests are routed to the appropriate chain based on the
 * {@code X-App-Origin} header or request path prefix. Unknown requests
 * default to the elatus chain (fail-secure).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({IpWhitelistProperties.class, RateLimitProperties.class})
public class SecurityConfig {

    /**
     * Security filter chain for akademia-plus-web requests.
     *
     * <p>Matches requests with the {@code X-App-Origin: akademia} header
     * or the {@code /akademia/**} path prefix. Applies standard JWT
     * validation and tenant context verification.</p>
     *
     * @param moduleSecurityConfigurators the set of module security configurators
     * @param http                        the HTTP security builder
     * @param jwtRequestFilter            the JWT request filter
     * @param appOriginFilter             the app origin filter
     * @return the akademia security filter chain
     * @throws Exception if configuration fails
     */
    @SuppressWarnings("java:S4502") // CSRF disabled: stateless JWT Bearer auth, no cookies/sessions
    @Bean
    @Order(1)
    @Profile({"dev", "local"})
    public SecurityFilterChain akademiaSecurityFilterChain(
            Set<ModuleSecurityConfigurator> moduleSecurityConfigurators,
            HttpSecurity http,
            JwtRequestFilter jwtRequestFilter,
            AppOriginFilter appOriginFilter,
            IpWhitelistFilter ipWhitelistFilter) throws Exception {

        http
                .securityMatcher(new AkademiaRequestMatcher())
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
                    configurePermittedEndpoints(auth);
                    try {
                        for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                            configurator.configure(auth, AppOriginContext.ORIGIN_AKADEMIA);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply module security configurations", e);
                    }
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(appOriginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(ipWhitelistFilter, JwtRequestFilter.class)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Security filter chain for elatusdev-web requests.
     *
     * <p>Matches all requests that do not match the akademia chain.
     * Applies the full security stack: JWT validation, tenant context,
     * and hook points for token binding, rate limiting, and HMAC
     * verification (to be implemented in future features).</p>
     *
     * @param moduleSecurityConfigurators the set of module security configurators
     * @param http                        the HTTP security builder
     * @param jwtRequestFilter            the JWT request filter
     * @param appOriginFilter             the app origin filter
     * @return the elatus security filter chain
     * @throws Exception if configuration fails
     */
    @SuppressWarnings("java:S4502") // CSRF disabled: stateless JWT Bearer auth, no cookies/sessions
    @Bean
    @Order(2)
    @Profile({"dev", "local"})
    public SecurityFilterChain elatusSecurityFilterChain(
            Set<ModuleSecurityConfigurator> moduleSecurityConfigurators,
            HttpSecurity http,
            JwtRequestFilter jwtRequestFilter,
            AppOriginFilter appOriginFilter,
            RateLimitingFilter rateLimitingFilter) throws Exception {

        http
                .securityMatcher("/**")
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
                    configurePermittedEndpoints(auth);
                    try {
                        for (ModuleSecurityConfigurator configurator : moduleSecurityConfigurators) {
                            configurator.configure(auth, AppOriginContext.ORIGIN_ELATUS);
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to apply module security configurations", e);
                    }
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(appOriginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitingFilter, JwtRequestFilter.class)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Configures endpoints that are permitted without authentication.
     *
     * <p>These rules are shared across both the akademia and elatus
     * security filter chains.</p>
     *
     * @param auth the authorization configuration registry
     */
    private void configurePermittedEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v1/security/login/internal").permitAll()
                .requestMatchers("/v1/security/register").permitAll()
                .requestMatchers("/v1/security/token/refresh").permitAll()
                .requestMatchers("/v1/security/logout").permitAll()
                .requestMatchers("/v1/security/passkey/login/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll();
    }

    /**
     * CORS configuration for dev/local profiles.
     *
     * @return the CORS configuration source
     */
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
        source.registerCorsConfiguration("/v1/security/passkey/login/**", loginCorsConfig);
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
     *
     * @return the CORS configuration source for mock-data-service
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

    /**
     * Request matcher that identifies akademia-plus-web requests.
     *
     * <p>Matches if the {@code X-App-Origin} header is "akademia" or
     * the request path starts with {@code /akademia/}.</p>
     *
     * @author ElatusDev
     * @since 1.0
     */
    private static class AkademiaRequestMatcher implements RequestMatcher {

        /**
         * Tests whether the request originates from akademia-plus-web.
         *
         * @param request the HTTP request
         * @return true if the request matches akademia criteria
         */
        @Override
        public boolean matches(HttpServletRequest request) {
            String header = request.getHeader(AppOriginContext.APP_ORIGIN_HEADER);
            if (AppOriginContext.ORIGIN_AKADEMIA.equalsIgnoreCase(header)) {
                return true;
            }
            String path = request.getRequestURI();
            return path.startsWith(AppOriginContext.PATH_PREFIX_AKADEMIA);
        }
    }
}
