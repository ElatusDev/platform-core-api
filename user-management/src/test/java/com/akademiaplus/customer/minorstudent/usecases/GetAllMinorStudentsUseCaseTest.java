/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllMinorStudentsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllMinorStudentsUseCaseTest {

    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllMinorStudentsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllMinorStudentsUseCase(minorStudentRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no minor students exist")
        void shouldReturnEmptyList_whenNoMinorStudentsExist() {
            // Given
            when(minorStudentRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetMinorStudentResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(minorStudentRepository).findAll();
            verifyNoMoreInteractions(minorStudentRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when minor students exist")
        void shouldReturnMappedDtos_whenMinorStudentsExist() {
            // Given
            PersonPIIDataModel personPII1 = new PersonPIIDataModel();
            PersonPIIDataModel personPII2 = new PersonPIIDataModel();
            MinorStudentDataModel minorStudent1 = new MinorStudentDataModel();
            minorStudent1.setPersonPII(personPII1);
            MinorStudentDataModel minorStudent2 = new MinorStudentDataModel();
            minorStudent2.setPersonPII(personPII2);
            GetMinorStudentResponseDTO dto1 = new GetMinorStudentResponseDTO();
            GetMinorStudentResponseDTO dto2 = new GetMinorStudentResponseDTO();

            when(minorStudentRepository.findAll()).thenReturn(List.of(minorStudent1, minorStudent2));
            when(modelMapper.map(minorStudent1, GetMinorStudentResponseDTO.class)).thenReturn(dto1);
            doNothing().when(modelMapper).map(personPII1, dto1);
            when(modelMapper.map(minorStudent2, GetMinorStudentResponseDTO.class)).thenReturn(dto2);
            doNothing().when(modelMapper).map(personPII2, dto2);

            // When
            List<GetMinorStudentResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(minorStudentRepository).findAll();
            verify(modelMapper).map(minorStudent1, GetMinorStudentResponseDTO.class);
            verify(modelMapper).map(personPII1, dto1);
            verify(modelMapper).map(minorStudent2, GetMinorStudentResponseDTO.class);
            verify(modelMapper).map(personPII2, dto2);
            verifyNoMoreInteractions(minorStudentRepository, modelMapper);
        }
    }
}
