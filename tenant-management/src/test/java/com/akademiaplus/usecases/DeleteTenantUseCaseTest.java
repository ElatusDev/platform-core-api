/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteTenantUseCase}.
 * <p>
 * Unlike other delete use cases, {@link DeleteTenantUseCase} operates with
 * a single {@code @Id} (Long) instead of a composite key, because
 * {@code TenantDataModel} IS the tenant root entity.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteTenantUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteTenantUseCaseTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private TenantRepository repository;

    private DeleteTenantUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTenantUseCase(repository);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete tenant when found by ID")
        void shouldSoftDeleteTenant_whenFoundById() {
            // Given
            TenantDataModel entity = new TenantDataModel();
            when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(TENANT_ID);

            // Then
            assertThat(entity).isNotNull();
            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository, times(1)).findById(TENANT_ID);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tenant missing")
        void shouldThrowEntityNotFound_whenTenantMissing() {
            // Given
            when(repository.findById(TENANT_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TENANT, String.valueOf(TENANT_ID)));
            verify(repository, times(1)).findById(TENANT_ID);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Constraint Violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violated")
        void shouldThrowDeletionNotAllowed_whenConstraintViolated() {
            // Given
            TenantDataModel entity = new TenantDataModel();
            when(repository.findById(TENANT_ID)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.TENANT, String.valueOf(TENANT_ID)));
            InOrder inOrder = inOrder(repository);
            inOrder.verify(repository, times(1)).findById(TENANT_ID);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository.findById throws")
        void shouldPropagateException_whenFindByIdThrows() {
            // Given
            when(repository.findById(TENANT_ID))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            verify(repository, times(1)).findById(TENANT_ID);
            verifyNoMoreInteractions(repository);
        }
    }
}
