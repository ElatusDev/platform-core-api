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
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.lead.management.dto.GetDemoRequestResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GetDemoRequestByIdUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetDemoRequestByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetDemoRequestByIdUseCaseTest {

    private static final Long DEMO_REQUEST_ID = 1L;

    @Mock
    private DemoRequestRepository demoRequestRepository;

    @Mock
    private ModelMapper modelMapper;

    private GetDemoRequestByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDemoRequestByIdUseCase(demoRequestRepository, modelMapper);
    }

    @Nested
    @DisplayName("Successful Retrieval")
    class SuccessfulRetrieval {

        @Test
        @DisplayName("Should return mapped DTO when demo request exists")
        void shouldReturnMappedDto_whenDemoRequestExists() {
            // Given
            DemoRequestDataModel model = new DemoRequestDataModel();
            model.setDemoRequestId(DEMO_REQUEST_ID);
            GetDemoRequestResponseDTO expectedDto = new GetDemoRequestResponseDTO();
            expectedDto.setDemoRequestId(DEMO_REQUEST_ID);

            when(demoRequestRepository.findById(DEMO_REQUEST_ID))
                    .thenReturn(Optional.of(model));
            when(modelMapper.map(model, GetDemoRequestResponseDTO.class))
                    .thenReturn(expectedDto);

            // When
            GetDemoRequestResponseDTO result = useCase.get(DEMO_REQUEST_ID);

            // Then
            assertThat(result.getDemoRequestId()).isEqualTo(DEMO_REQUEST_ID);
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when demo request does not exist")
        void shouldThrowEntityNotFoundException_whenDemoRequestDoesNotExist() {
            // Given
            when(demoRequestRepository.findById(DEMO_REQUEST_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.get(DEMO_REQUEST_ID))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }
}
