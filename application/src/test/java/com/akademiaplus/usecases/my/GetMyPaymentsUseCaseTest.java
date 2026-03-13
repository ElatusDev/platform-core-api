/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import openapi.akademiaplus.domain.my.dto.MyPaymentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetMyPaymentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyPaymentsUseCaseTest {

    private static final Long PROFILE_ID = 100L;
    private static final Long PAYMENT_ID = 200L;
    private static final BigDecimal AMOUNT = new BigDecimal("99.50");
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 1, 15);
    private static final String PAYMENT_METHOD = "credit_card";

    @Mock private UserContextHolder userContextHolder;
    @Mock private PaymentAdultStudentRepository paymentAdultStudentRepository;

    private GetMyPaymentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyPaymentsUseCase(userContextHolder, paymentAdultStudentRepository);
    }

    private PaymentAdultStudentDataModel buildPayment() {
        PaymentAdultStudentDataModel payment = new PaymentAdultStudentDataModel();
        payment.setPaymentAdultStudentId(PAYMENT_ID);
        payment.setAmount(AMOUNT);
        payment.setPaymentDate(PAYMENT_DATE);
        payment.setPaymentMethod(PAYMENT_METHOD);
        return payment;
    }

    @Nested
    @DisplayName("Payment Retrieval")
    class PaymentRetrieval {

        @Test
        @DisplayName("Should return payments when student has payment history")
        void shouldReturnPayments_whenPaymentsExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(paymentAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(buildPayment()));

            // When
            List<MyPaymentDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPaymentAdultStudentId()).isEqualTo(PAYMENT_ID);
            assertThat(result.get(0).getAmount()).isEqualTo(AMOUNT.doubleValue());
            assertThat(result.get(0).getPaymentDate()).isEqualTo(PAYMENT_DATE);
            assertThat(result.get(0).getPaymentMethod()).isEqualTo(PAYMENT_METHOD);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, paymentAdultStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(paymentAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when no payments exist")
        void shouldReturnEmptyList_whenNoPaymentsExist() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(paymentAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyPaymentDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(paymentAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, paymentAdultStudentRepository);
        }

        @Test
        @DisplayName("Should handle null amount gracefully")
        void shouldHandleNullAmount_whenAmountNotSet() {
            // Given
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            PaymentAdultStudentDataModel payment = new PaymentAdultStudentDataModel();
            payment.setPaymentAdultStudentId(PAYMENT_ID);
            payment.setPaymentDate(PAYMENT_DATE);
            payment.setPaymentMethod(PAYMENT_METHOD);
            when(paymentAdultStudentRepository.findByAdultStudentId(PROFILE_ID))
                    .thenReturn(List.of(payment));

            // When
            List<MyPaymentDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isNull();

            // Then — interactions
            verify(paymentAdultStudentRepository, times(1)).findByAdultStudentId(PROFILE_ID);
        }
    }
}
