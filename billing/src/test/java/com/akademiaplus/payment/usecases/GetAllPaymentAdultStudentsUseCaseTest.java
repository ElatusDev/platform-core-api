/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllPaymentAdultStudentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllPaymentAdultStudentsUseCaseTest {

    @Mock private PaymentAdultStudentRepository paymentAdultStudentRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllPaymentAdultStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllPaymentAdultStudentsUseCase(paymentAdultStudentRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no payment adult students exist")
        void shouldReturnEmptyList_whenNoPaymentAdultStudentsExist() {
            // Given
            when(paymentAdultStudentRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetPaymentAdultStudentResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(paymentAdultStudentRepository).findAll();
            verifyNoMoreInteractions(paymentAdultStudentRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when payment adult students exist")
        void shouldReturnMappedDtos_whenPaymentAdultStudentsExist() {
            // Given
            PaymentAdultStudentDataModel paymentAdultStudent1 = new PaymentAdultStudentDataModel();
            PaymentAdultStudentDataModel paymentAdultStudent2 = new PaymentAdultStudentDataModel();
            GetPaymentAdultStudentResponseDTO dto1 = new GetPaymentAdultStudentResponseDTO();
            GetPaymentAdultStudentResponseDTO dto2 = new GetPaymentAdultStudentResponseDTO();
            when(paymentAdultStudentRepository.findAll()).thenReturn(List.of(paymentAdultStudent1, paymentAdultStudent2));
            when(modelMapper.map(paymentAdultStudent1, GetPaymentAdultStudentResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(paymentAdultStudent2, GetPaymentAdultStudentResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetPaymentAdultStudentResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(paymentAdultStudentRepository).findAll();
            verify(modelMapper).map(paymentAdultStudent1, GetPaymentAdultStudentResponseDTO.class);
            verify(modelMapper).map(paymentAdultStudent2, GetPaymentAdultStudentResponseDTO.class);
            verifyNoMoreInteractions(paymentAdultStudentRepository, modelMapper);
        }
    }
}
