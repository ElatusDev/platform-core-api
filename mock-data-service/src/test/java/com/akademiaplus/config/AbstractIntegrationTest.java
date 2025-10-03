package com.akademiaplus.config;

import com.akademiaplus.MockDataApp;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(classes = {MockDataApp.class})
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.2")
            .withDatabaseName("multi_tenant_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("00-schema-dev.sql"); // Your existing schema file!

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MariaDBDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}