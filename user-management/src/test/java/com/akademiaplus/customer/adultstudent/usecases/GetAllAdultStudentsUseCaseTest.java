/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetAdultStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllAdultStudentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllAdultStudentsUseCaseTest {

    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllAdultStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllAdultStudentsUseCase(adultStudentRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no adult students exist")
        void shouldReturnEmptyList_whenNoAdultStudentsExist() {
            // Given
            when(adultStudentRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetAdultStudentResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();

            verify(adultStudentRepository, times(1)).findAll();
            verifyNoMoreInteractions(adultStudentRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when adult students exist")
        void shouldReturnMappedDtos_whenAdultStudentsExist() {
            // Given
            PersonPIIDataModel personPII1 = new PersonPIIDataModel();
            PersonPIIDataModel personPII2 = new PersonPIIDataModel();
            AdultStudentDataModel adultStudent1 = new AdultStudentDataModel();
            adultStudent1.setPersonPII(personPII1);
            AdultStudentDataModel adultStudent2 = new AdultStudentDataModel();
            adultStudent2.setPersonPII(personPII2);
            GetAdultStudentResponseDTO dto1 = new GetAdultStudentResponseDTO();
            GetAdultStudentResponseDTO dto2 = new GetAdultStudentResponseDTO();

            when(adultStudentRepository.findAll()).thenReturn(List.of(adultStudent1, adultStudent2));
            when(modelMapper.map(adultStudent1, GetAdultStudentResponseDTO.class)).thenReturn(dto1);
            doNothing().when(modelMapper).map(personPII1, dto1);
            when(modelMapper.map(adultStudent2, GetAdultStudentResponseDTO.class)).thenReturn(dto2);
            doNothing().when(modelMapper).map(personPII2, dto2);

            // When
            List<GetAdultStudentResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);

            InOrder inOrder = inOrder(adultStudentRepository, modelMapper);
            inOrder.verify(adultStudentRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(adultStudent1, GetAdultStudentResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(personPII1, dto1);
            inOrder.verify(modelMapper, times(1)).map(adultStudent2, GetAdultStudentResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(personPII2, dto2);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
