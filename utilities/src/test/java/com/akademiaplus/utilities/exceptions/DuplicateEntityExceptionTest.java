/*
 * Copyright (c) 2026 ElatusDev
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
 * Unit tests for {@link DuplicateEntityException}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DuplicateEntityException")
class DuplicateEntityExceptionTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String DUPLICATE_FIELD = "email";

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("Should store entityType and field correctly")
        void shouldStoreEntityTypeAndField_whenConstructed() {
            // Given
            Throwable cause = new RuntimeException("Unique constraint violated");

            // When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD, cause);

            // Then
            assertThat(exception.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(exception.getField()).isEqualTo(DUPLICATE_FIELD);
        }

        @Test
        @DisplayName("Should store cause correctly")
        void shouldStoreCause_whenConstructed() {
            // Given
            Throwable cause = new RuntimeException("Unique constraint violated");

            // When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD, cause);

            // Then
            assertThat(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("Should format message using MESSAGE_TEMPLATE")
        void shouldFormatMessageUsingTemplate_whenConstructed() {
            // Given
            Throwable cause = new RuntimeException("Unique constraint violated");

            // When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD, cause);

            // Then
            assertThat(exception.getMessage())
                    .isEqualTo(String.format(
                            DuplicateEntityException.MESSAGE_TEMPLATE, DUPLICATE_FIELD, ENTITY_TYPE));
        }
    }

    @Nested
    @DisplayName("Two-arg Constructor")
    class TwoArgConstructor {

        @Test
        @DisplayName("Should store entityType and field correctly without cause")
        void shouldStoreEntityTypeAndField_whenConstructedWithoutCause() {
            // Given & When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD);

            // Then
            assertThat(exception.getEntityType()).isEqualTo(ENTITY_TYPE);
            assertThat(exception.getField()).isEqualTo(DUPLICATE_FIELD);
        }

        @Test
        @DisplayName("Should have null cause when constructed without cause")
        void shouldHaveNullCause_whenConstructedWithoutCause() {
            // Given & When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD);

            // Then
            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("Should format message using MESSAGE_TEMPLATE without cause")
        void shouldFormatMessageUsingTemplate_whenConstructedWithoutCause() {
            // Given & When
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD);

            // Then
            assertThat(exception.getMessage())
                    .isEqualTo(String.format(
                            DuplicateEntityException.MESSAGE_TEMPLATE, DUPLICATE_FIELD, ENTITY_TYPE));
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("Should return exact entityType passed to constructor")
        void shouldReturnExactEntityType_whenGetEntityTypeCalled() {
            // Given
            Throwable cause = new RuntimeException("Unique constraint violated");
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD, cause);

            // When
            String result = exception.getEntityType();

            // Then
            assertThat(result).isEqualTo(ENTITY_TYPE);
        }

        @Test
        @DisplayName("Should return exact field passed to constructor")
        void shouldReturnExactField_whenGetFieldCalled() {
            // Given
            Throwable cause = new RuntimeException("Unique constraint violated");
            DuplicateEntityException exception =
                    new DuplicateEntityException(ENTITY_TYPE, DUPLICATE_FIELD, cause);

            // When
            String result = exception.getField();

            // Then
            assertThat(result).isEqualTo(DUPLICATE_FIELD);
        }
    }
}
