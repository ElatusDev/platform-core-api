/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

import org.mockito.InOrder;

/**
 * Unit tests for {@link DeleteMembershipUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteMembershipUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteMembershipUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MEMBERSHIP_ID = 42L;

    @Mock
    private MembershipRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteMembershipUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteMembershipUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete membership when found by composite key")
        void shouldSoftDeleteMembership_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            MembershipDataModel entity = new MembershipDataModel();
            MembershipDataModel.MembershipCompositeId compositeId =
                    new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(MEMBERSHIP_ID);

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
        @DisplayName("Should throw EntityNotFoundException when membership missing")
        void shouldThrowEntityNotFound_whenMembershipMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            MembershipDataModel.MembershipCompositeId compositeId =
                    new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(MEMBERSHIP_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.MEMBERSHIP);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(MEMBERSHIP_ID));
                    });

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(repository, times(1)).findById(compositeId);
            verifyNoMoreInteractions(tenantContextHolder, repository);
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
            MembershipDataModel entity = new MembershipDataModel();
            MembershipDataModel.MembershipCompositeId compositeId =
                    new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(MEMBERSHIP_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.MEMBERSHIP);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(MEMBERSHIP_ID));
                        assertThat(edna.getReason()).isNull();
                    });

            InOrder inOrder = inOrder(tenantContextHolder, repository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(repository, times(1)).findById(compositeId);
            inOrder.verify(repository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
