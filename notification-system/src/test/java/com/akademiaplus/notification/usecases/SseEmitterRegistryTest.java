/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SseEmitterRegistry")
class SseEmitterRegistryTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long OTHER_USER_ID = 200L;
    private static final String EXPECTED_KEY = "1:100";

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Should return new SseEmitter when registering a user")
        void shouldReturnNewEmitter_whenRegistering() {
            // Given — a tenant and user ID

            // When
            SseEmitter emitter = registry.register(TENANT_ID, USER_ID);

            // Then
            assertThat(emitter).isNotNull();
        }

        @Test
        @DisplayName("Should store emitter retrievable by same tenant and user")
        void shouldStoreEmitter_whenRegistering() {
            // Given
            SseEmitter registered = registry.register(TENANT_ID, USER_ID);

            // When
            Optional<SseEmitter> retrieved = registry.getEmitter(TENANT_ID, USER_ID);

            // Then
            assertThat(retrieved).isPresent().contains(registered);
        }

        @Test
        @DisplayName("Should replace existing emitter when same user registers again")
        void shouldReplaceEmitter_whenSameUserRegistersAgain() {
            // Given
            SseEmitter first = registry.register(TENANT_ID, USER_ID);

            // When
            SseEmitter second = registry.register(TENANT_ID, USER_ID);

            // Then
            assertThat(second).isNotSameAs(first);
            assertThat(registry.getEmitter(TENANT_ID, USER_ID)).contains(second);
        }
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return emitter when user is registered")
        void shouldReturnEmitter_whenUserIsRegistered() {
            // Given
            SseEmitter emitter = registry.register(TENANT_ID, USER_ID);

            // When
            Optional<SseEmitter> result = registry.getEmitter(TENANT_ID, USER_ID);

            // Then
            assertThat(result).isPresent().contains(emitter);
        }

        @Test
        @DisplayName("Should return empty when user is not registered")
        void shouldReturnEmpty_whenUserNotRegistered() {
            // Given — no registration

            // When
            Optional<SseEmitter> result = registry.getEmitter(TENANT_ID, OTHER_USER_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Removal")
    class Removal {

        @Test
        @DisplayName("Should remove emitter when calling remove")
        void shouldRemoveEmitter_whenCallingRemove() {
            // Given
            registry.register(TENANT_ID, USER_ID);

            // When
            registry.remove(TENANT_ID, USER_ID);

            // Then
            assertThat(registry.getEmitter(TENANT_ID, USER_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Key Building")
    class KeyBuilding {

        @Test
        @DisplayName("Should build key with tenant ID and user ID separated by colon")
        void shouldBuildKey_withTenantAndUserId() {
            // Given — TENANT_ID and USER_ID constants

            // When
            String key = registry.buildKey(TENANT_ID, USER_ID);

            // Then
            assertThat(key).isEqualTo(EXPECTED_KEY);
        }
    }
}
