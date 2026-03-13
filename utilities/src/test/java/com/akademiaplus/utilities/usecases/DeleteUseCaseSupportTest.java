/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.usecases;

import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteUseCaseSupport}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteUseCaseSupport")
@ExtendWith(MockitoExtension.class)
class DeleteUseCaseSupportTest {

    private static final String ENTITY_TYPE = EntityType.EMPLOYEE;
    private static final String ENTITY_ID = "42";
    private static final Long COMPOSITE_ID = 42L;

    @Mock
    private TenantScopedRepository<Object, Long> repository;

    @Nested
    @DisplayName("executeDelete")
    class ExecuteDelete {

        @Test
        @DisplayName("Should delete entity when found by composite ID")
        void shouldDeleteEntity_whenFoundByCompositeId() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));

            // When
            DeleteUseCaseSupport.executeDelete(repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID);

            // Then
            verify(repository, times(1)).findById(COMPOSITE_ID);
            verify(repository, times(1)).delete(entity);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when entity not found")
        void shouldThrowEntityNotFound_whenEntityMissing() {
            // Given
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                    DeleteUseCaseSupport.executeDelete(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(
                            EntityNotFoundException.MESSAGE_TEMPLATE, ENTITY_TYPE, ENTITY_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(ENTITY_TYPE);
                        assertThat(enfe.getEntityId()).isEqualTo(ENTITY_ID);
                    });

            verify(repository, times(1)).findById(COMPOSITE_ID);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violation occurs")
        void shouldThrowDeletionNotAllowed_whenConstraintViolation() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));
            DataIntegrityViolationException cause =
                    new DataIntegrityViolationException("FK constraint");
            doThrow(cause).when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() ->
                    DeleteUseCaseSupport.executeDelete(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(
                            EntityDeletionNotAllowedException.MESSAGE_TEMPLATE, ENTITY_TYPE, ENTITY_ID))
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(ENTITY_TYPE);
                        assertThat(edna.getEntityId()).isEqualTo(ENTITY_ID);
                        assertThat(edna.getReason()).isNull();
                        assertThat(edna.getCause()).isSameAs(cause);
                    });

            verify(repository, times(1)).findById(COMPOSITE_ID);
            verify(repository, times(1)).delete(entity);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("findOrThrow")
    class FindOrThrow {

        @Test
        @DisplayName("Should return entity when found")
        void shouldReturnEntity_whenFound() {
            // Given
            Object entity = new Object();
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.of(entity));

            // When
            Object result = DeleteUseCaseSupport.findOrThrow(
                    repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID);

            // Then
            assertThat(result).isSameAs(entity);
            verify(repository, times(1)).findById(COMPOSITE_ID);
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when not found")
        void shouldThrowEntityNotFound_whenNotFound() {
            // Given: repository returns empty
            when(repository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When/Then: should throw EntityNotFoundException with correct fields
            assertThatThrownBy(() ->
                    DeleteUseCaseSupport.findOrThrow(
                            repository, COMPOSITE_ID, ENTITY_TYPE, ENTITY_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(
                            EntityNotFoundException.MESSAGE_TEMPLATE, ENTITY_TYPE, ENTITY_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(ENTITY_TYPE);
                        assertThat(enfe.getEntityId()).isEqualTo(ENTITY_ID);
                    });

            // Rule 9: only findById called, no downstream operations
            verify(repository, times(1)).findById(COMPOSITE_ID);
            verifyNoMoreInteractions(repository);
        }
    }
}
