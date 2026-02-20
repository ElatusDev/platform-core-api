/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.infra.persistence.config.TenantContextLoader;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Tenant configuration for the internal mock-data pipeline.
 *
 * <p>Provides a singleton {@link TenantContextHolder} and disables the
 * {@link TenantContextLoader} servlet filter. In production the holder is
 * {@code @RequestScope} and the filter extracts the tenant from the
 * {@code X-Tenant-Id} header. The mock-data service is an internal
 * infrastructure tool (invoked by CI pipelines or local scripts) that
 * creates tenants from scratch, so neither the header nor request-scoped
 * holder apply.</p>
 *
 * <p>The {@code @Primary} singleton replaces the request-scoped bean,
 * allowing the orchestrator to set the tenant once and have every
 * {@link com.akademiaplus.infra.persistence.listeners.TenantPreInsertEventListener}
 * invocation see the same value.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@Profile("mock-data-service")
public class MockDataTenantConfiguration {

    /**
     * Singleton tenant context holder for the mock-data pipeline.
     *
     * @return a thread-safe {@link TenantContextHolder} singleton
     */
    @Bean
    @Primary
    public TenantContextHolder tenantContextHolder() {
        return new TenantContextHolder();
    }

    /**
     * Disables the {@link TenantContextLoader} servlet filter.
     *
     * <p>The mock-data service is internal infrastructure — it creates
     * tenants as part of its pipeline and manages tenant context via the
     * orchestrator. The {@code X-Tenant-Id} header requirement does not
     * apply to this service.</p>
     *
     * @param filter the auto-detected {@link TenantContextLoader} component
     * @return a registration that prevents the filter from being applied
     */
    @Bean
    public FilterRegistrationBean<TenantContextLoader> disableTenantContextLoader(
            TenantContextLoader filter) {
        FilterRegistrationBean<TenantContextLoader> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
