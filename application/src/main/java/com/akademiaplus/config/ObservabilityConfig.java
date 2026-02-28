/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

/**
 * Observability configuration for metrics tagging and instrumentation.
 *
 * <p>Adds a {@code tenantId} low-cardinality key to all HTTP server observations
 * so that Prometheus metrics can be sliced by tenant. The tenant ID is read from
 * the {@code X-Tenant-Id} request header at observation time.
 */
@Configuration
public class ObservabilityConfig {

    public static final String TENANT_TAG_KEY = "tenantId";
    public static final String TENANT_TAG_DEFAULT = "none";
    public static final String TENANT_HEADER = "X-Tenant-Id";

    /**
     * Adds the tenant ID as a low-cardinality key to HTTP server observations.
     *
     * @return observation filter that tags metrics with tenantId
     */
    @Bean
    public ObservationFilter tenantObservationFilter() {
        return context -> {
            if (context instanceof ServerRequestObservationContext serverContext) {
                String tenantId = serverContext.getCarrier().getHeader(TENANT_HEADER);
                context.addLowCardinalityKeyValue(KeyValue.of(
                        TENANT_TAG_KEY,
                        tenantId != null ? tenantId : TENANT_TAG_DEFAULT
                ));
            }
            return context;
        };
    }
}
