/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.utilities.security.AESGCMEncryptionService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test-only configuration that provides an {@link AESGCMEncryptionService}
 * bean with a hardcoded test encryption key.
 *
 * <p>This is needed because Hibernate's {@code SpringBeanContainer} instantiates
 * JPA converters (like {@code StringEncryptor}) during {@code entityManagerFactory}
 * initialization, before Spring's {@code @Value} resolution is available for
 * converter dependencies. Providing an explicit bean ensures the encryption
 * service is ready before Hibernate needs it.
 *
 * @author ElatusDev
 * @since 1.0
 */
@TestConfiguration
public class TestSecurityConfiguration {

    private static final String TEST_ENCRYPTION_KEY = "zZhnG8Pe0W9bOHWNDrqTNHC0sDIdVHEsCW/jJWPt1cI=";

    @Bean
    @Primary
    public AESGCMEncryptionService aesGCMEncryptionService() {
        return new AESGCMEncryptionService(TEST_ENCRYPTION_KEY);
    }
}
