/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.MockDataApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for mock-data-service integration tests.
 *
 * <p>Extends the shared {@link com.akademiaplus.infra.testing.AbstractIntegrationTest}
 * from {@code infra-common} (test-jar) and adds the mock-data-service–specific
 * Spring Boot configuration: application class and active profile.
 *
 * <p>Subclasses inherit the Testcontainers MariaDB instance, dynamic property
 * registration, encryption and tenant test configurations from the parent.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootTest(
        classes = {MockDataApp.class},
        properties = "spring.main.allow-bean-definition-overriding=true"
)
@ActiveProfiles("mock-data-service")
public abstract class AbstractIntegrationTest
        extends com.akademiaplus.infra.testing.AbstractIntegrationTest {
}
