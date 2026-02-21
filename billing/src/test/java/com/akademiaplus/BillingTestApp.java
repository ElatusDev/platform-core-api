/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * Minimal Spring Boot application class for billing integration tests.
 *
 * <p>Placed in the {@code com.akademiaplus} root package so that Spring Boot's
 * auto-configuration defaults (entity scanning, JPA repository detection) cover
 * all transitive dependencies (infra-common, multi-tenant-data, security,
 * utilities) alongside billing's own beans.
 *
 * <p>{@link TenantContextHolder} is excluded from component scanning because
 * its production {@code @RequestScope} creates a CGLIB scoped proxy that
 * prevents test code from setting the tenant context directly. The test-jar
 * {@link com.akademiaplus.infra.testing.IntegrationTestTenantConfiguration}
 * provides a singleton replacement via {@code @Bean @Primary}.
 *
 * <p>This class is NOT a production artifact — it exists solely to provide the
 * application context required by {@code @SpringBootTest} in component tests.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootApplication
@ComponentScan(
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = TenantContextHolder.class
        )
)
public class BillingTestApp {
}
