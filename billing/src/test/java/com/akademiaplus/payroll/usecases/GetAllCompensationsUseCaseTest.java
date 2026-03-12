/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

@DisplayName("GetAllCompensationsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllCompensationsUseCaseTest {

    @Mock private CompensationRepository compensationRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllCompensationsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllCompensationsUseCase(compensationRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no compensations exist")
        void shouldReturnEmptyList_whenNoCompensationsExist() {
            // Given
            when(compensationRepository.findAll()).thenReturn(Collections.emptyList());
            // When
            List<GetCompensationResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).isEmpty();
            verify(compensationRepository, times(1)).findAll();
            verifyNoMoreInteractions(compensationRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when compensations exist")
        void shouldReturnMappedDtos_whenCompensationsExist() {
            // Given
            CompensationDataModel compensation1 = new CompensationDataModel();
            CompensationDataModel compensation2 = new CompensationDataModel();
            GetCompensationResponseDTO dto1 = new GetCompensationResponseDTO();
            GetCompensationResponseDTO dto2 = new GetCompensationResponseDTO();
            when(compensationRepository.findAll()).thenReturn(List.of(compensation1, compensation2));
            when(modelMapper.map(compensation1, GetCompensationResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(compensation2, GetCompensationResponseDTO.class)).thenReturn(dto2);
            // When
            List<GetCompensationResponseDTO> result = useCase.getAll();
            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(compensationRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(compensation1, GetCompensationResponseDTO.class);
            verify(modelMapper, times(1)).map(compensation2, GetCompensationResponseDTO.class);
            verifyNoMoreInteractions(compensationRepository, modelMapper);
        }
    }
}
