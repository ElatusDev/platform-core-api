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
 * Unit tests for {@link EntityDeletionNotAllowedException}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("EntityDeletionNotAllowedException")
class EntityDeletionNotAllowedExceptionTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String ENTITY_ID = "42";
    private static final String BUSINESS_REASON = "Tutor tiene 3 alumno(s) menor(es) activo(s)";

    @Nested
    @DisplayName("DB Constraint Constructor")
    class DbConstraintConstructor {

        @Test
        @DisplayName("Should store entityType and entityId when DB constraint violation")
        void shouldStoreEntityTypeAndEntityId_whenDbConstraintViolation() {
            // Given
            Throwable cause = new RuntimeException("FK constraint violated");

            // When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, cause);

            // Then
            assertThat(exception.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(exception.getEntityId()).isEqualTo(ENTITY_ID);
        }

        @Test
        @DisplayName("Should set reason to null when DB constraint violation")
        void shouldSetReasonToNull_whenDbConstraintViolation() {
            // Given
            Throwable cause = new RuntimeException("FK constraint violated");

            // When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, cause);

            // Then
            assertThat(exception.getReason()).isNull();
        }

        @Test
        @DisplayName("Should store cause when DB constraint violation")
        void shouldStoreCause_whenDbConstraintViolation() {
            // Given
            Throwable cause = new RuntimeException("FK constraint violated");

            // When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, cause);

            // Then
            assertThat(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Should include entity info in message when DB constraint violation")
        void shouldIncludeEntityInfoInMessage_whenDbConstraintViolation() {
            // Given
            Throwable cause = new RuntimeException("FK constraint violated");

            // When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, cause);

            // Then
            assertThat(exception.getMessage()).contains(ENTITY_TYPE);
            assertThat(exception.getMessage()).contains(ENTITY_ID);
        }
    }

    @Nested
    @DisplayName("Business Rule Constructor")
    class BusinessRuleConstructor {

        @Test
        @DisplayName("Should store entityType and entityId when business rule violation")
        void shouldStoreEntityTypeAndEntityId_whenBusinessRuleViolation() {
            // Given & When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, BUSINESS_REASON);

            // Then
            assertThat(exception.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(exception.getEntityId()).isEqualTo(ENTITY_ID);
        }

        @Test
        @DisplayName("Should store reason when business rule violation")
        void shouldStoreReason_whenBusinessRuleViolation() {
            // Given & When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, BUSINESS_REASON);

            // Then
            assertThat(exception.getReason()).isEqualTo(BUSINESS_REASON);
        }

        @Test
        @DisplayName("Should have null cause when business rule violation")
        void shouldHaveNullCause_whenBusinessRuleViolation() {
            // Given & When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, BUSINESS_REASON);

            // Then
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should include entity info and reason in message when business rule violation")
        void shouldIncludeEntityInfoAndReasonInMessage_whenBusinessRuleViolation() {
            // Given & When
            EntityDeletionNotAllowedException exception =
                    new EntityDeletionNotAllowedException(ENTITY_TYPE, ENTITY_ID, BUSINESS_REASON);

            // Then
            assertThat(exception.getMessage()).contains(ENTITY_TYPE);
            assertThat(exception.getMessage()).contains(ENTITY_ID);
            assertThat(exception.getMessage()).contains(BUSINESS_REASON);
        }
    }
}
