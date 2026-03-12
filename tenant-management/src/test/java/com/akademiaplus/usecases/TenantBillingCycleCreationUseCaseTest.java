/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDTO;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TenantBillingCycleCreationUseCase")
@ExtendWith(MockitoExtension.class)
class TenantBillingCycleCreationUseCaseTest {

    @Mock private TenantBillingCycleRepository repository;
    @Mock private ApplicationContext applicationContext;
    @Mock private ModelMapper modelMapper;

    private TenantBillingCycleCreationUseCase useCase;

    private static final String BILLING_MONTH = "2026-03";
    private static final LocalDate CALCULATION_DATE = LocalDate.of(2026, 3, 15);
    private static final Integer USER_COUNT = 42;
    private static final Double TOTAL_AMOUNT = 6300.0;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new TenantBillingCycleCreationUseCase(applicationContext, repository, modelMapper);
    }

    private BillingCycleCreateRequestDTO buildDto() {
        BillingCycleCreateRequestDTO dto = new BillingCycleCreateRequestDTO();
        dto.setBillingMonth(BILLING_MONTH);
        dto.setCalculationDate(CALCULATION_DATE);
        dto.setUserCount(USER_COUNT);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype TenantBillingCycleDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantBillingCycleDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantBillingCycleDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should parse billingMonth string to LocalDate using first day of month")
        void shouldParseBillingMonth_whenStringIsProvided() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantBillingCycleDataModel result = useCase.transform(dto);

            // Then — "2026-03" -> LocalDate(2026, 3, 1)
            assertThat(result.getBillingMonth()).isEqualTo(LocalDate.of(2026, 3, 1));
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should leave billingMonth null when DTO billingMonth is null")
        void shouldLeaveBillingMonthNull_whenDtoBillingMonthIsNull() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            dto.setBillingMonth(null);
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantBillingCycleDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getBillingMonth()).isNull();
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            TenantBillingCycleDataModel savedModel = new TenantBillingCycleDataModel();
            savedModel.setTenantBillingCycleId(SAVED_ID);
            BillingCycleDTO expectedDto = new BillingCycleDTO();
            expectedDto.setTenantBillingCycleId(SAVED_ID);

            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, BillingCycleDTO.class)).thenReturn(expectedDto);

            // When
            BillingCycleDTO result = useCase.create(dto);

            // Then
            assertThat(result.getTenantBillingCycleId()).isEqualTo(SAVED_ID);
            InOrder inOrder = inOrder(applicationContext, modelMapper, repository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verify(repository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, BillingCycleDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            BillingCycleCreateRequestDTO dto = buildDto();
            TenantBillingCycleDataModel prototypeModel = new TenantBillingCycleDataModel();
            TenantBillingCycleDataModel savedModel = new TenantBillingCycleDataModel();
            BillingCycleDTO responseDto = new BillingCycleDTO();

            when(applicationContext.getBean(TenantBillingCycleDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, BillingCycleDTO.class)).thenReturn(responseDto);

            // When
            BillingCycleDTO result = useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, repository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantBillingCycleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantBillingCycleCreationUseCase.MAP_NAME);
            inOrder.verify(repository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, BillingCycleDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
