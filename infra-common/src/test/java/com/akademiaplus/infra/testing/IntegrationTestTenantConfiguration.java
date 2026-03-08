/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.testing;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.config.TenantContextLoader;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Shared test tenant configuration for integration tests.
 *
 * <p>Provides a singleton {@link TenantContextHolder} (instead of the
 * production {@code @RequestScope} bean) and disables the
 * {@link TenantContextLoader} servlet filter. Integration tests set
 * the tenant context directly via {@code tenantContextHolder.setTenantId()}.
 *
 * <p>Activated by the {@code mock-data-service} profile so that component
 * scanning auto-discovers this configuration without requiring explicit
 * {@code @Import} on the test class.
 *
 * <p>Published via the {@code infra-common} test-jar.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@Profile("mock-data-service")
public class IntegrationTestTenantConfiguration {

    /**
     * Singleton tenant context holder for integration tests.
     *
     * @return a singleton {@link TenantContextHolder} that persists across calls
     */
    @Bean
    @Primary
    public TenantContextHolder tenantContextHolder() {
        return new TenantContextHolder();
    }

    /**
     * Disables the {@link TenantContextLoader} servlet filter so that
     * integration tests do not require the {@code X-Tenant-Id} header.
     *
     * @param filter the auto-detected {@link TenantContextLoader} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<TenantContextLoader> disableTenantContextLoaderForTests(
            TenantContextLoader filter) {
        FilterRegistrationBean<TenantContextLoader> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code JwtRequestFilter} servlet filter so that
     * integration tests do not require JWT authentication headers.
     *
     * <p>Uses {@link Qualifier} by bean name because the {@code security}
     * module is not a compile dependency of {@code infra-common}.
     *
     * @param filter the auto-detected {@code JwtRequestFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableJwtRequestFilterForTests(
            @Qualifier("jwtRequestFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code IpWhitelistFilter} servlet filter so that
     * integration tests are not subject to IP-based access control.
     *
     * <p>Uses {@link Qualifier} by bean name because the {@code security}
     * module is not a compile dependency of {@code infra-common}.
     *
     * @param filter the auto-detected {@code IpWhitelistFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableIpWhitelistFilterForTests(
            @Qualifier("ipWhitelistFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code HmacSigningFilter} servlet filter so that
     * integration tests do not require HMAC request signing.
     *
     * @param filter the auto-detected {@code HmacSigningFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableHmacSigningFilterForTests(
            @Qualifier("hmacSigningFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code HmacResponseFilter} servlet filter so that
     * integration tests do not add HMAC response signatures.
     *
     * @param filter the auto-detected {@code HmacResponseFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableHmacResponseFilterForTests(
            @Qualifier("hmacResponseFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code RateLimitingFilter} servlet filter so that
     * integration tests are not subject to rate limiting.
     *
     * @param filter the auto-detected {@code RateLimitingFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableRateLimitingFilterForTests(
            @Qualifier("rateLimitingFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Disables the {@code TokenBindingFilter} servlet filter so that
     * integration tests do not enforce device fingerprint binding.
     *
     * @param filter the auto-detected {@code TokenBindingFilter} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<Filter> disableTokenBindingFilterForTests(
            @Qualifier("tokenBindingFilter") Filter filter) {
        FilterRegistrationBean<Filter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
