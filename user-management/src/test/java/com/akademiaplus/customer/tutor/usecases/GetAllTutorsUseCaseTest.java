/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
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

@DisplayName("GetAllTutorsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllTutorsUseCaseTest {

    @Mock private TutorRepository tutorRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllTutorsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllTutorsUseCase(tutorRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no tutors exist")
        void shouldReturnEmptyList_whenNoTutorsExist() {
            // Given
            when(tutorRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetTutorResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(tutorRepository, times(1)).findAll();
            verifyNoMoreInteractions(tutorRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when tutors exist")
        void shouldReturnMappedDtos_whenTutorsExist() {
            // Given
            PersonPIIDataModel personPII1 = new PersonPIIDataModel();
            PersonPIIDataModel personPII2 = new PersonPIIDataModel();
            TutorDataModel tutor1 = new TutorDataModel();
            tutor1.setPersonPII(personPII1);
            TutorDataModel tutor2 = new TutorDataModel();
            tutor2.setPersonPII(personPII2);
            GetTutorResponseDTO dto1 = new GetTutorResponseDTO();
            GetTutorResponseDTO dto2 = new GetTutorResponseDTO();

            when(tutorRepository.findAll()).thenReturn(List.of(tutor1, tutor2));
            when(modelMapper.map(tutor1, GetTutorResponseDTO.class)).thenReturn(dto1);
            doNothing().when(modelMapper).map(personPII1, dto1);
            when(modelMapper.map(tutor2, GetTutorResponseDTO.class)).thenReturn(dto2);
            doNothing().when(modelMapper).map(personPII2, dto2);

            // When
            List<GetTutorResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(tutorRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(tutor1, GetTutorResponseDTO.class);
            verify(modelMapper, times(1)).map(personPII1, dto1);
            verify(modelMapper, times(1)).map(tutor2, GetTutorResponseDTO.class);
            verify(modelMapper, times(1)).map(personPII2, dto2);
            verifyNoMoreInteractions(tutorRepository, modelMapper);
        }
    }
}
