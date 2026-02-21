/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.BillingTestApp;
import com.akademiaplus.infra.testing.IntegrationTestTenantConfiguration;
import com.akademiaplus.infra.testing.TestSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for billing integration tests.
 *
 * <p>Extends the shared {@link com.akademiaplus.infra.testing.AbstractIntegrationTest}
 * from {@code infra-common} (test-jar) and adds the module-specific Spring Boot
 * configuration: test application class and {@code mock-data-service} profile
 * to disable authentication/authorization.
 *
 * <p>Subclasses inherit the Testcontainers MariaDB instance, dynamic property
 * registration, encryption and tenant test configurations from the parent.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootTest(
        classes = {BillingTestApp.class},
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("mock-data-service")
@Import({TestSecurityConfiguration.class, IntegrationTestTenantConfiguration.class})
public abstract class AbstractIntegrationTest
        extends com.akademiaplus.infra.testing.AbstractIntegrationTest {
}
