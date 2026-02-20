/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteCollaboratorUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteCollaboratorUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteCollaboratorUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COLLABORATOR_ID = 42L;

    @Mock
    private CollaboratorRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteCollaboratorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteCollaboratorUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete collaborator when found by composite key")
        void shouldSoftDeleteCollaborator_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CollaboratorDataModel entity = new CollaboratorDataModel();
            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(COLLABORATOR_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when collaborator missing")
        void shouldThrowEntityNotFound_whenCollaboratorMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(COLLABORATOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(COLLABORATOR_ID));
                    });
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
            CollaboratorDataModel entity = new CollaboratorDataModel();
            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(COLLABORATOR_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(COLLABORATOR_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
