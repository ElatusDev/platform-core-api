/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.testing;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import java.net.URL;
import java.util.Objects;

/**
 * Shared base class for integration tests across all modules.
 *
 * <p>Provides a Testcontainers-managed MariaDB instance initialized with the
 * development schema. All required Spring properties (datasource, JWT,
 * encryption, payment gateway) are injected dynamically so that tests
 * run without external environment variables.
 *
 * <p>Uses the Singleton Container Pattern so that a single MariaDB instance
 * is shared across all test classes in the same JVM. The container starts
 * once when this class is loaded and Testcontainers' Ryuk handles cleanup.
 *
 * <p>This class is intentionally NOT annotated with {@code @SpringBootTest}
 * — each module must add that annotation with its own application class
 * (or test-only {@code @SpringBootApplication}).
 *
 * <p>The JWT keystore path is resolved from the classpath to an absolute
 * file path at runtime because {@code KeyLoader} uses raw
 * {@code FileInputStream} rather than Spring's resource abstraction.
 *
 * <p>Published via the {@code infra-common} test-jar so that domain modules
 * can extend this base class for their own component tests.
 *
 * @author ElatusDev
 * @since 1.0
 */
public abstract class AbstractIntegrationTest {

    private static final String MARIADB_IMAGE = "mariadb:11.2";
    private static final String DATABASE_NAME = "multi_tenant_db";
    private static final String DATABASE_USER = "test_user";
    private static final String DATABASE_PASSWORD = "test_password";
    private static final String INIT_SCRIPT = "00-schema-dev.sql";

    private static final String TEST_KEYSTORE_RESOURCE = "test-keystore.p12";

    protected static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(MARIADB_IMAGE)
            .withDatabaseName(DATABASE_NAME)
            .withUsername(DATABASE_USER)
            .withPassword(DATABASE_PASSWORD)
            .withInitScript(INIT_SCRIPT);

    static {
        mariaDB.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registerDatasourceProperties(registry);
        registerJwtKeystorePath(registry);
        registerTestProperties(registry);
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

    /**
     * Registers additional test properties that would normally come from
     * environment variables or profile-specific property files.
     */
    private static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.keystore.password", () -> "testpassword");
        registry.add("jwt.keystore.alias", () -> "test-jwt");
        registry.add("jwt.token.validity-ms", () -> "900000");
        registry.add("security.encryption-key",
                () -> "zZhnG8Pe0W9bOHWNDrqTNHC0sDIdVHEsCW/jJWPt1cI=");
        registry.add("mp.access.token",
                () -> "TEST-0000000000000000-000000-ffffffffffffffffffffffffffffffff-000000000");
        registry.add("spring.main.allow-bean-definition-overriding", () -> "true");
    }
}
