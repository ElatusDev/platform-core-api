/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.usecases;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.payroll.interfaceadapters.CompensationRepository;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CompensationCreationUseCase")
@ExtendWith(MockitoExtension.class)
class CompensationCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private CompensationRepository compensationRepository;
    @Mock private ModelMapper modelMapper;

    private CompensationCreationUseCase useCase;

    private static final String COMPENSATION_TYPE = "HOURLY";
    private static final Double AMOUNT = 50.0;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new CompensationCreationUseCase(applicationContext, compensationRepository, modelMapper);
    }

    private CompensationCreationRequestDTO buildDto() {
        CompensationCreationRequestDTO dto = new CompensationCreationRequestDTO();
        dto.setCompensationType(COMPENSATION_TYPE);
        dto.setAmount(AMOUNT);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype CompensationDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            CompensationCreationRequestDTO dto = buildDto();
            CompensationDataModel prototypeModel = new CompensationDataModel();
            when(applicationContext.getBean(CompensationDataModel.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(CompensationDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            CompensationCreationRequestDTO dto = buildDto();
            CompensationDataModel prototypeModel = new CompensationDataModel();
            when(applicationContext.getBean(CompensationDataModel.class)).thenReturn(prototypeModel);

            // When
            CompensationDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, CompensationCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            CompensationCreationRequestDTO dto = buildDto();
            CompensationDataModel prototypeModel = new CompensationDataModel();
            CompensationDataModel savedModel = new CompensationDataModel();
            savedModel.setCompensationId(SAVED_ID);
            CompensationCreationResponseDTO expectedDto = new CompensationCreationResponseDTO();
            expectedDto.setCompensationId(SAVED_ID);

            when(applicationContext.getBean(CompensationDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, CompensationCreationUseCase.MAP_NAME);
            when(compensationRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, CompensationCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            CompensationCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(compensationRepository).save(prototypeModel);
            verify(modelMapper).map(savedModel, CompensationCreationResponseDTO.class);
            assertThat(result.getCompensationId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            CompensationCreationRequestDTO dto = buildDto();
            CompensationDataModel prototypeModel = new CompensationDataModel();
            CompensationDataModel savedModel = new CompensationDataModel();
            CompensationCreationResponseDTO responseDto = new CompensationCreationResponseDTO();

            when(applicationContext.getBean(CompensationDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, CompensationCreationUseCase.MAP_NAME);
            when(compensationRepository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, CompensationCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, compensationRepository);
            inOrder.verify(applicationContext).getBean(CompensationDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, CompensationCreationUseCase.MAP_NAME);
            inOrder.verify(compensationRepository).save(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, CompensationCreationResponseDTO.class);
        }
    }
}
