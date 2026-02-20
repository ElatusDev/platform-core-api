/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.exception.PaymentAdultStudentNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetPaymentAdultStudentByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetPaymentAdultStudentByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PAYMENT_ADULT_STUDENT_ID = 100L;

    @Mock private PaymentAdultStudentRepository paymentAdultStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetPaymentAdultStudentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPaymentAdultStudentByIdUseCase(paymentAdultStudentRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenPaymentAdultStudentFound() {
            // Given
            PaymentAdultStudentDataModel paymentAdultStudent = new PaymentAdultStudentDataModel();
            GetPaymentAdultStudentResponseDTO expectedDto = new GetPaymentAdultStudentResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(paymentAdultStudentRepository.findById(new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(paymentAdultStudent));
            when(modelMapper.map(paymentAdultStudent, GetPaymentAdultStudentResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetPaymentAdultStudentResponseDTO result = useCase.get(PAYMENT_ADULT_STUDENT_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(paymentAdultStudentRepository).findById(new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID));
            verify(modelMapper).map(paymentAdultStudent, GetPaymentAdultStudentResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, paymentAdultStudentRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw NotFoundException when entity not found")
        void shouldThrowPaymentAdultStudentNotFoundException_whenPaymentAdultStudentNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(paymentAdultStudentRepository.findById(new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(PAYMENT_ADULT_STUDENT_ID))
                    .isInstanceOf(PaymentAdultStudentNotFoundException.class)
                    .hasMessage(String.valueOf(PAYMENT_ADULT_STUDENT_ID));
            verify(tenantContextHolder).getTenantId();
            verify(paymentAdultStudentRepository).findById(new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(TENANT_ID, PAYMENT_ADULT_STUDENT_ID));
            verifyNoMoreInteractions(tenantContextHolder, paymentAdultStudentRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {
        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(PAYMENT_ADULT_STUDENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetPaymentAdultStudentByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, paymentAdultStudentRepository, modelMapper);
        }
    }
}
