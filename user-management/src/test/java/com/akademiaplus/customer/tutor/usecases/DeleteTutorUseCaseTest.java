/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.TutorDataModel;
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
 * Unit tests for {@link DeleteTutorUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteTutorUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteTutorUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TUTOR_ID = 42L;
    private static final TutorDataModel.TutorCompositeId COMPOSITE_ID =
            new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

    @Mock
    private TutorRepository tutorRepository;

    @Mock
    private MinorStudentRepository minorStudentRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteTutorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteTutorUseCase(
                tutorRepository, minorStudentRepository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete tutor when found and no active minor students")
        void shouldSoftDeleteTutor_whenFoundAndNoActiveMinorStudents() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TutorDataModel tutor = new TutorDataModel();
            when(tutorRepository.findById(COMPOSITE_ID)).thenReturn(Optional.of(tutor));
            when(minorStudentRepository.countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID))
                    .thenReturn(0L);

            // When
            useCase.delete(TUTOR_ID);

            // Then
            assertThat(tutor).isNotNull();

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(COMPOSITE_ID);
            inOrder.verify(minorStudentRepository, times(1)).countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID);
            inOrder.verify(tutorRepository, times(1)).delete(tutor);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tutor missing")
        void shouldThrowEntityNotFound_whenTutorMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tutorRepository.findById(COMPOSITE_ID)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TUTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TUTOR, TUTOR_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                    });

            verifyNoInteractions(minorStudentRepository);
        }
    }

    @Nested
    @DisplayName("Business Rule — Active Minor Students")
    class ActiveMinorStudentsRule {

        @Test
        @DisplayName("Should throw EntityDeletionNotAllowed when tutor has active minor students")
        void shouldThrowDeletionNotAllowed_whenTutorHasActiveMinorStudents() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            TutorDataModel tutor = new TutorDataModel();
            when(tutorRepository.findById(COMPOSITE_ID)).thenReturn(Optional.of(tutor));
            when(minorStudentRepository.countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID))
                    .thenReturn(3L);

            // When / Then
            String expectedReason = String.format(DeleteTutorUseCase.ACTIVE_MINOR_STUDENTS_REASON, 3L);

            assertThatThrownBy(() -> useCase.delete(TUTOR_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE_WITH_REASON,
                            EntityType.TUTOR, TUTOR_ID, expectedReason))
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                        assertThat(edna.getReason()).isEqualTo(expectedReason);
                        assertThat(edna.getCause()).isNull();
                    });

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(COMPOSITE_ID);
            inOrder.verify(minorStudentRepository, times(1)).countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID);
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
            TutorDataModel tutor = new TutorDataModel();
            when(tutorRepository.findById(COMPOSITE_ID)).thenReturn(Optional.of(tutor));
            when(minorStudentRepository.countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID))
                    .thenReturn(0L);
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(tutorRepository).delete(tutor);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(TUTOR_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.TUTOR, TUTOR_ID))
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                        assertThat(edna.getReason()).isNull();
                        assertThat(edna.getCause())
                                .isInstanceOf(DataIntegrityViolationException.class);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(COMPOSITE_ID);
            inOrder.verify(minorStudentRepository, times(1)).countByTenantIdAndTutorId(TENANT_ID, TUTOR_ID);
            inOrder.verify(tutorRepository, times(1)).delete(tutor);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
