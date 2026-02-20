/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
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
 * Unit tests for {@link DeletePaymentAdultStudentUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeletePaymentAdultStudentUseCase")
@ExtendWith(MockitoExtension.class)
class DeletePaymentAdultStudentUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PAYMENT_ADULT_STUDENT_ID = 42L;

    @Mock
    private PaymentAdultStudentRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeletePaymentAdultStudentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePaymentAdultStudentUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete payment adult student when found by composite key")
        void shouldSoftDeletePaymentAdultStudent_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            PaymentAdultStudentDataModel entity = new PaymentAdultStudentDataModel();
            PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId compositeId =
                    new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(PAYMENT_ADULT_STUDENT_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when payment adult student missing")
        void shouldThrowEntityNotFound_whenPaymentAdultStudentMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId compositeId =
                    new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(PAYMENT_ADULT_STUDENT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.PAYMENT_ADULT_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(PAYMENT_ADULT_STUDENT_ID));
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
            PaymentAdultStudentDataModel entity = new PaymentAdultStudentDataModel();
            PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId compositeId =
                    new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(PAYMENT_ADULT_STUDENT_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.PAYMENT_ADULT_STUDENT);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(PAYMENT_ADULT_STUDENT_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
