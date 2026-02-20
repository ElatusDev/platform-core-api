/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
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
 * Unit tests for {@link DeletePaymentTutorUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DeletePaymentTutorUseCase")
@ExtendWith(MockitoExtension.class)
class DeletePaymentTutorUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PAYMENT_TUTOR_ID = 42L;

    @Mock
    private PaymentTutorRepository repository;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private DeletePaymentTutorUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeletePaymentTutorUseCase(repository, tenantContextHolder);
    }

    @Nested
    @DisplayName("Successful Deletion")
    class SuccessfulDeletion {

        @Test
        @DisplayName("Should soft-delete payment tutor when found by composite key")
        void shouldSoftDeletePaymentTutor_whenFoundByCompositeKey() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            PaymentTutorDataModel entity = new PaymentTutorDataModel();
            PaymentTutorDataModel.PaymentTutorCompositeId compositeId =
                    new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));

            // When
            useCase.delete(PAYMENT_TUTOR_ID);

            // Then
            verify(repository).delete(entity);
        }
    }

    @Nested
    @DisplayName("Entity Not Found")
    class EntityNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when payment tutor missing")
        void shouldThrowEntityNotFound_whenPaymentTutorMissing() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            PaymentTutorDataModel.PaymentTutorCompositeId compositeId =
                    new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.delete(PAYMENT_TUTOR_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.PAYMENT_TUTOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(PAYMENT_TUTOR_ID));
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
            PaymentTutorDataModel entity = new PaymentTutorDataModel();
            PaymentTutorDataModel.PaymentTutorCompositeId compositeId =
                    new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID);
            when(repository.findById(compositeId)).thenReturn(Optional.of(entity));
            doThrow(new DataIntegrityViolationException("FK constraint"))
                    .when(repository).delete(entity);

            // When / Then
            assertThatThrownBy(() -> useCase.delete(PAYMENT_TUTOR_ID))
                    .isInstanceOf(EntityDeletionNotAllowedException.class)
                    .satisfies(ex -> {
                        EntityDeletionNotAllowedException edna =
                                (EntityDeletionNotAllowedException) ex;
                        assertThat(edna.getEntityType()).isEqualTo(EntityType.PAYMENT_TUTOR);
                        assertThat(edna.getEntityId()).isEqualTo(String.valueOf(PAYMENT_TUTOR_ID));
                        assertThat(edna.getReason()).isNull();
                    });
        }
    }
}
