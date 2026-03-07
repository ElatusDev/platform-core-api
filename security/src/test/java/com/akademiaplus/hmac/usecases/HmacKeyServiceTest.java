/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

import com.akademiaplus.hmac.exceptions.HmacSignatureException;
import com.akademiaplus.hmac.interfaceadapters.config.HmacProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HmacKeyService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("HmacKeyService")
@ExtendWith(MockitoExtension.class)
class HmacKeyServiceTest {

    private static final String TEST_KEY = "test-hmac-key-32-chars-minimum!!";
    private static final String CUSTOM_APP_ID = "custom-app";
    private static final String CUSTOM_KEY = "custom-app-signing-key-value!!!";

    @Mock
    private HmacProperties hmacProperties;

    private HmacKeyService service;

    @BeforeEach
    void setUp() {
        service = new HmacKeyService(hmacProperties);
    }

    @Nested
    @DisplayName("Key resolution")
    class KeyResolution {

        @Test
        @DisplayName("should return key when appId is configured")
        void shouldReturnKey_whenAppIdConfigured() {
            // Given
            Map<String, String> keys = new HashMap<>();
            keys.put(CUSTOM_APP_ID, CUSTOM_KEY);
            when(hmacProperties.getKeys()).thenReturn(keys);

            // When
            byte[] result = service.resolveKey(CUSTOM_APP_ID);

            // Then
            assertThat(result).isEqualTo(CUSTOM_KEY.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should fall back to default key when appId not found")
        void shouldReturnDefaultKey_whenAppIdNotFound() {
            // Given
            Map<String, String> keys = new HashMap<>();
            keys.put(HmacKeyService.DEFAULT_APP_ID, TEST_KEY);
            when(hmacProperties.getKeys()).thenReturn(keys);

            // When
            byte[] result = service.resolveKey("unknown-app");

            // Then
            assertThat(result).isEqualTo(TEST_KEY.getBytes(StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should throw when no key is configured")
        void shouldThrowException_whenNoKeyConfigured() {
            // Given
            when(hmacProperties.getKeys()).thenReturn(new HashMap<>());

            // When / Then
            assertThatThrownBy(() -> service.resolveKey("unknown-app"))
                    .isInstanceOf(HmacSignatureException.class)
                    .hasMessageContaining("unknown-app");
        }

        @Test
        @DisplayName("should resolve default key for elatusdev-web")
        void shouldResolveDefaultKey() {
            // Given
            Map<String, String> keys = new HashMap<>();
            keys.put(HmacKeyService.DEFAULT_APP_ID, TEST_KEY);
            when(hmacProperties.getKeys()).thenReturn(keys);

            // When
            byte[] result = service.resolveDefaultKey();

            // Then
            assertThat(result).isEqualTo(TEST_KEY.getBytes(StandardCharsets.UTF_8));
        }
    }
}
