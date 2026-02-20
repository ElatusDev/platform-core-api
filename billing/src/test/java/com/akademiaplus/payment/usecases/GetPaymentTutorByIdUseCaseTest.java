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

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.exception.PaymentTutorNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import java.util.Optional;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetPaymentTutorByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetPaymentTutorByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PAYMENT_TUTOR_ID = 100L;

    @Mock private PaymentTutorRepository paymentTutorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetPaymentTutorByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPaymentTutorByIdUseCase(paymentTutorRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {
        @Test
        @DisplayName("Should return mapped DTO when entity found")
        void shouldReturnMappedDto_whenPaymentTutorFound() {
            // Given
            PaymentTutorDataModel paymentTutor = new PaymentTutorDataModel();
            GetPaymentTutorResponseDTO expectedDto = new GetPaymentTutorResponseDTO();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(paymentTutorRepository.findById(new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID)))
                    .thenReturn(Optional.of(paymentTutor));
            when(modelMapper.map(paymentTutor, GetPaymentTutorResponseDTO.class)).thenReturn(expectedDto);
            // When
            GetPaymentTutorResponseDTO result = useCase.get(PAYMENT_TUTOR_ID);
            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(paymentTutorRepository).findById(new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID));
            verify(modelMapper).map(paymentTutor, GetPaymentTutorResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, paymentTutorRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {
        @Test
        @DisplayName("Should throw NotFoundException when entity not found")
        void shouldThrowPaymentTutorNotFoundException_whenPaymentTutorNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(paymentTutorRepository.findById(new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID)))
                    .thenReturn(Optional.empty());
            // When & Then
            assertThatThrownBy(() -> useCase.get(PAYMENT_TUTOR_ID))
                    .isInstanceOf(PaymentTutorNotFoundException.class)
                    .hasMessage(String.valueOf(PAYMENT_TUTOR_ID));
            verify(tenantContextHolder).getTenantId();
            verify(paymentTutorRepository).findById(new PaymentTutorDataModel.PaymentTutorCompositeId(TENANT_ID, PAYMENT_TUTOR_ID));
            verifyNoMoreInteractions(tenantContextHolder, paymentTutorRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(PAYMENT_TUTOR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetPaymentTutorByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, paymentTutorRepository, modelMapper);
        }
    }
}
