/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllPaymentTutorsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllPaymentTutorsUseCaseTest {

    @Mock private PaymentTutorRepository paymentTutorRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllPaymentTutorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllPaymentTutorsUseCase(paymentTutorRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no payment tutors exist")
        void shouldReturnEmptyList_whenNoPaymentTutorsExist() {
            // Given
            when(paymentTutorRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetPaymentTutorResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(paymentTutorRepository, times(1)).findAll();
            verifyNoMoreInteractions(paymentTutorRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when payment tutors exist")
        void shouldReturnMappedDtos_whenPaymentTutorsExist() {
            // Given
            PaymentTutorDataModel paymentTutor1 = new PaymentTutorDataModel();
            PaymentTutorDataModel paymentTutor2 = new PaymentTutorDataModel();
            GetPaymentTutorResponseDTO dto1 = new GetPaymentTutorResponseDTO();
            GetPaymentTutorResponseDTO dto2 = new GetPaymentTutorResponseDTO();
            when(paymentTutorRepository.findAll()).thenReturn(List.of(paymentTutor1, paymentTutor2));
            when(modelMapper.map(paymentTutor1, GetPaymentTutorResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(paymentTutor2, GetPaymentTutorResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetPaymentTutorResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(paymentTutorRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(paymentTutor1, GetPaymentTutorResponseDTO.class);
            verify(modelMapper, times(1)).map(paymentTutor2, GetPaymentTutorResponseDTO.class);
            verifyNoMoreInteractions(paymentTutorRepository, modelMapper);
        }
    }
}
