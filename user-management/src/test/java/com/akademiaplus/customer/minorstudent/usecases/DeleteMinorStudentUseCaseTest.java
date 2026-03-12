/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.MinorStudentDataModel;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeleteMinorStudentUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteMinorStudentUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteMinorStudentUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MINOR_STUDENT_ID = 42L;

    @Mock
    private MinorStudentRepository minorStudentRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteMinorStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteMinorStudentUseCase(minorStudentRepository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete minor student when found by composite key")
        void shouldSoftDeleteMinorStudent_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            MinorStudentDataModel entity = new MinorStudentDataModel();
            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(MINOR_STUDENT_ID);

            // Then
            assertThat(entity).isNotNull();

            InOrder inOrder = inOrder(tenantContextHolder, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(minorStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(minorStudentRepository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when minor student missing")
        void shouldThrowEntityNotFound_whenMinorStudentMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(MINOR_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(MINOR_STUDENT_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(minorStudentRepository, times(1)).findById(compositeId);
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
            MinorStudentDataModel entity = new MinorStudentDataModel();
            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(minorStudentRepository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(MINOR_STUDENT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(MINOR_STUDENT_ID));
                        assertThat(edna.getReason()).isNull();
                    });

            InOrder inOrder = inOrder(tenantContextHolder, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(minorStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(minorStudentRepository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
