/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.infra.testing;

import com.akademiaplus.utilities.security.AESGCMEncryptionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Shared test configuration that provides an {@link AESGCMEncryptionService}
 * bean with a hardcoded test encryption key.
 *
 * <p>This is needed because Hibernate's {@code SpringBeanContainer} instantiates
 * JPA converters (like {@code StringEncryptor}) during {@code entityManagerFactory}
 * initialization, before Spring's {@code @Value} resolution is available for
 * converter dependencies. Providing an explicit bean ensures the encryption
 * service is ready before Hibernate needs it.
 *
 * <p>Activated by the {@code mock-data-service} profile so that component
 * scanning auto-discovers this configuration without requiring explicit
 * {@code @Import} on the test class.
 *
 * <p>Published via the {@code infra-common} test-jar.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@Profile("mock-data-service")
public class TestSecurityConfiguration {

    private static final String TEST_ENCRYPTION_KEY = "zZhnG8Pe0W9bOHWNDrqTNHC0sDIdVHEsCW/jJWPt1cI=";

    /**
     * Provides a test encryption service with a hardcoded key.
     *
     * @return the AES-GCM encryption service configured for testing
     */
    @Bean
    @Primary
    public AESGCMEncryptionService aesGCMEncryptionService() {
        return new AESGCMEncryptionService(TEST_ENCRYPTION_KEY);
    }
}
