/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.exceptions;

import com.akademiaplus.utilities.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EntityNotFoundException}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("EntityNotFoundException")
class EntityNotFoundExceptionTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String ENTITY_ID = "42";

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should store entityType and entityId correctly")
        void shouldStoreEntityTypeAndEntityId_whenConstructed() {
            // Given & When
            EntityNotFoundException exception = new EntityNotFoundException(ENTITY_TYPE, ENTITY_ID);

            // Then
            assertThat(exception.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(exception.getEntityId()).isEqualTo(ENTITY_ID);
        }

        @Test
        @DisplayName("Should include entityType and entityId in message")
        void shouldIncludeEntityTypeAndEntityIdInMessage_whenConstructed() {
            // Given & When
            EntityNotFoundException exception = new EntityNotFoundException(ENTITY_TYPE, ENTITY_ID);

            // Then
            assertThat(exception.getMessage()).contains(ENTITY_TYPE);
            assertThat(exception.getMessage()).contains(ENTITY_ID);
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("Should return exact entityType passed to constructor")
        void shouldReturnExactEntityType_whenGetEntityTypeCalled() {
            // Given
            EntityNotFoundException exception = new EntityNotFoundException(ENTITY_TYPE, ENTITY_ID);

            // When
            String result = exception.getEntityType();

            // Then
            assertThat(result).isEqualTo(ENTITY_TYPE);
        }

        @Test
        @DisplayName("Should return exact entityId passed to constructor")
        void shouldReturnExactEntityId_whenGetEntityIdCalled() {
            // Given
            EntityNotFoundException exception = new EntityNotFoundException(ENTITY_TYPE, ENTITY_ID);

            // When
            String result = exception.getEntityId();

            // Then
            assertThat(result).isEqualTo(ENTITY_ID);
        }
    }
}
