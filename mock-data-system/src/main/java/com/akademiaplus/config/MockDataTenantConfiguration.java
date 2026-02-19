/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Provides a singleton {@link TenantContextHolder} for the mock-data pipeline.
 *
 * <p>In production the holder is {@code @RequestScope} — the servlet filter
 * extracts the tenant from the JWT and calls {@code setTenantId()}.
 * The mock-data pipeline runs outside a web request, so the request-scoped
 * bean cannot be resolved. This {@code @Primary} singleton replaces it,
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

    @Bean
    @Primary
    public TenantContextHolder tenantContextHolder() {
        return new TenantContextHolder();
    }
}
