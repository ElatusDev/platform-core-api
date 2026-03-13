/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.exceptions.InvalidTenantException;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteTenantSubscriptionUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteTenantSubscriptionUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteTenantSubscriptionUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TENANT_SUBSCRIPTION_ID = 42L;

    @Mock
    private TenantSubscriptionRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteTenantSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTenantSubscriptionUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete tenant subscription when found by composite key")
        void shouldSoftDeleteTenantSubscription_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TenantSubscriptionDataModel entity = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, TENANT_SUBSCRIPTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(TENANT_SUBSCRIPTION_ID);

            // Then
            assertThat(entity).isNotNull();
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tenant subscription missing")
        void shouldThrowEntityNotFound_whenTenantSubscriptionMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, TENANT_SUBSCRIPTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_SUBSCRIPTION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TENANT_SUBSCRIPTION, String.valueOf(TENANT_SUBSCRIPTION_ID)));
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Constraint Violation")
    class ConstraintViolation {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when constraint violated")
        void shouldThrowDeletionNotAllowed_whenConstraintViolated() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TenantSubscriptionDataModel entity = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, TENANT_SUBSCRIPTION_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_SUBSCRIPTION_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.TENANT_SUBSCRIPTION, String.valueOf(TENANT_SUBSCRIPTION_ID)));
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate InvalidTenantException when tenant context missing")
        void shouldPropagateException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.requireTenantId())
                    .thenThrow(new InvalidTenantException());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_SUBSCRIPTION_ID))
                    .isInstanceOf(InvalidTenantException.class)
                    .hasMessage(InvalidTenantException.DEFAULT_MESSAGE);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should propagate exception when repository.findById throws")
        void shouldPropagateException_whenFindByIdThrows() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId compositeId =
                    new TenantSubscriptionDataModel.TenantSubscriptionCompositeId(TENANT_ID, TENANT_SUBSCRIPTION_ID);
            when(repository.findById(compositeId))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TENANT_SUBSCRIPTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");
            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
