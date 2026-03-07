/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.usecases;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.leadmanagement.interfaceadapters.DemoRequestRepository;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetAllDemoRequestsUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetAllDemoRequestsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllDemoRequestsUseCaseTest {

    @Mock
    private DemoRequestRepository demoRequestRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetAllDemoRequestsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllDemoRequestsUseCase(demoRequestRepository, modelMapper);
    }

    @Nested
    @DisplayName("Successful Retrieval")
    class SuccessfulRetrieval {

        @Test
        @DisplayName("Should return all demo requests mapped to DTOs")
        void shouldReturnAllDemoRequests_mappedToDtos() {
            // Given
            DemoRequestDataModel model1 = new DemoRequestDataModel();
            model1.setDemoRequestId(1L);
            DemoRequestDataModel model2 = new DemoRequestDataModel();
            model2.setDemoRequestId(2L);

            GetDemoRequestResponseDTO dto1 = new GetDemoRequestResponseDTO();
            dto1.setDemoRequestId(1L);
            GetDemoRequestResponseDTO dto2 = new GetDemoRequestResponseDTO();
            dto2.setDemoRequestId(2L);

            when(demoRequestRepository.findAll()).thenReturn(List.of(model1, model2));
            when(modelMapper.map(model1, GetDemoRequestResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(model2, GetDemoRequestResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetDemoRequestResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDemoRequestId()).isEqualTo(1L);
            assertThat(result.get(1).getDemoRequestId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Should return empty list when no demo requests exist")
        void shouldReturnEmptyList_whenNoDemoRequestsExist() {
            // Given
            when(demoRequestRepository.findAll()).thenReturn(List.of());

            // When
            List<GetDemoRequestResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
        }
    }
}
