/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.AdultStudentDataModel;
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
 * Unit tests for {@link DeleteAdultStudentUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeleteAdultStudentUseCase")
@ExtendWith(MockitoExtension.class)
class DeleteAdultStudentUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ADULT_STUDENT_ID = 42L;

    @Mock
    private AdultStudentRepository adultStudentRepository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeleteAdultStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteAdultStudentUseCase(adultStudentRepository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete adult student when found by composite key")
        void shouldSoftDeleteAdultStudent_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            AdultStudentDataModel entity = new AdultStudentDataModel();
            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(ADULT_STUDENT_ID);

            // Then
            assertThat(entity).isNotNull();

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(adultStudentRepository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when adult student missing")
        void shouldThrowEntityNotFound_whenAdultStudentMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ADULT_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.ADULT_STUDENT, ADULT_STUDENT_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(ADULT_STUDENT_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
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
            AdultStudentDataModel entity = new AdultStudentDataModel();
            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(adultStudentRepository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(ADULT_STUDENT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .hasMessage(String.format(EntityDeletionNotAllowedException.MESSAGE_TEMPLATE,
                            EntityType.ADULT_STUDENT, ADULT_STUDENT_ID))
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(ADULT_STUDENT_ID));
                        assertThat(edna.getReason()).isNull();
                    });

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(adultStudentRepository, times(1)).delete(entity);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
