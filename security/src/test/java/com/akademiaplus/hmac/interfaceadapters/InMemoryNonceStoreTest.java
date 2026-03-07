/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.interfaceadapters;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryNonceStore}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("InMemoryNonceStore")
class InMemoryNonceStoreTest {

    private static final String NONCE = "test-nonce-uuid";
    private static final long TTL_SECONDS = 300L;

    private InMemoryNonceStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryNonceStore();
    }

    @Nested
    @DisplayName("Nonce tracking")
    class NonceTracking {

        @Test
        @DisplayName("should return false when nonce is not stored")
        void shouldReturnFalse_whenNonceNotStored() {
            // When / Then
            assertThat(store.exists(NONCE)).isFalse();
        }

        @Test
        @DisplayName("should return true when nonce is already stored")
        void shouldReturnTrue_whenNonceAlreadyStored() {
            // Given
            store.store(NONCE, TTL_SECONDS);

            // When / Then
            assertThat(store.exists(NONCE)).isTrue();
        }

        @Test
        @DisplayName("should return false when nonce has expired")
        void shouldReturnFalse_whenNonceExpired() throws InterruptedException {
            // Given — store with 1-second TTL
            store.store(NONCE, 1L);
            assertThat(store.exists(NONCE)).isTrue();

            // When — wait for expiry
            Thread.sleep(1100L);

            // Then
            assertThat(store.exists(NONCE)).isFalse();
        }
    }
}
