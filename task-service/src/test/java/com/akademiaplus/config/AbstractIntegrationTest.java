/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.TaskTestApp;
import com.akademiaplus.infra.testing.IntegrationTestTenantConfiguration;
import com.akademiaplus.infra.testing.TestSecurityConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for task-service integration tests.
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
        classes = {TaskTestApp.class},
        properties = {
                "spring.main.allow-bean-definition-overriding=true",
                "jwt.refresh-token.validity-ms=2592000000",
                "security.cookie.domain=localhost",
                "security.cookie.secure=false",
                "security.cookie.access-token-name=access_token",
                "security.cookie.refresh-token-name=refresh_token",
                "security.cookie.access-token-max-age-seconds=900",
                "security.cookie.refresh-token-max-age-seconds=2592000",
                "akademia.email.host=localhost",
                "akademia.email.port=587",
                "akademia.email.username=test",
                "akademia.email.password=test",
                "akademia.email.from-address=test@test.com",
                "akademia.email.from-name=Test",
                "akademia.email.ses-configuration-set=",
                "app.phone.default-region=MX"
        }
)
@ActiveProfiles("mock-data-service")
@Import({TestSecurityConfiguration.class, IntegrationTestTenantConfiguration.class})
public abstract class AbstractIntegrationTest
        extends com.akademiaplus.infra.testing.AbstractIntegrationTest {
}
