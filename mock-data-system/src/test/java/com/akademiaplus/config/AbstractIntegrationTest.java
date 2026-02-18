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
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URL;
import java.util.Objects;

/**
 * Base class for mock-data-system integration tests.
 *
 * <p>Provides a Testcontainers-managed MariaDB instance initialized with the
 * development schema. All required Spring properties (datasource, JWT,
 * encryption, payment gateway) are injected dynamically so that tests
 * run without external environment variables.
 *
 * <p>The JWT keystore path is resolved from the classpath to an absolute
 * file path at runtime because {@code KeyLoader} uses raw
 * {@code FileInputStream} rather than Spring's resource abstraction.
 *
 * <p>Subclasses inherit the full Spring context with profile
 * {@code mock-data-service} and can focus exclusively on endpoint or
 * use-case verification.
 *
 * @author ElatusDev
 * @since 1.0
 */
@SpringBootTest(classes = {MockDataApp.class})
@ActiveProfiles("mock-data-service")
@Import(TestSecurityConfiguration.class)
@Testcontainers
public abstract class AbstractIntegrationTest {

    private static final String MARIADB_IMAGE = "mariadb:11.2";
    private static final String DATABASE_NAME = "multi_tenant_db";
    private static final String DATABASE_USER = "test_user";
    private static final String DATABASE_PASSWORD = "test_password";
    private static final String INIT_SCRIPT = "00-schema-dev.sql";

    private static final String TEST_KEYSTORE_RESOURCE = "test-keystore.p12";

    @Container
    protected static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(MARIADB_IMAGE)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USER)
            .withPassword(DATABASE_PASSWORD)
            .withInitScript(INIT_SCRIPT);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registerDatasourceProperties(registry);
        registerJwtKeystorePath(registry);
    }

    private static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MariaDBDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    /**
     * Resolves the test keystore from classpath to an absolute file path.
     * Required because {@code KeyLoader} uses {@code FileInputStream} directly.
     */
    private static void registerJwtKeystorePath(DynamicPropertyRegistry registry) {
        registry.add("jwt.keystore.path", () -> {
            URL resource = AbstractIntegrationTest.class.getClassLoader().getResource(TEST_KEYSTORE_RESOURCE);
            Objects.requireNonNull(resource, "Test keystore not found on classpath: " + TEST_KEYSTORE_RESOURCE);
            return resource.getPath();
        });
    }
}
