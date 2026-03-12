/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
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

import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("TenantSubscriptionCreationUseCase")
@ExtendWith(MockitoExtension.class)
class TenantSubscriptionCreationUseCaseTest {

    @Mock private TenantSubscriptionRepository repository;
    @Mock private ApplicationContext applicationContext;
    @Mock private ModelMapper modelMapper;

    private TenantSubscriptionCreationUseCase useCase;

    private static final String SUBSCRIPTION_TYPE = "standard";
    private static final LocalDate BILLING_DATE = LocalDate.of(2026, 3, 1);
    private static final Double RATE_PER_STUDENT = 150.0;
    private static final Integer MAX_USERS = 100;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new TenantSubscriptionCreationUseCase(applicationContext, repository, modelMapper);
    }

    private SubscriptionCreateRequestDTO buildDto() {
        SubscriptionCreateRequestDTO dto = new SubscriptionCreateRequestDTO();
        dto.setType(SubscriptionCreateRequestDTO.TypeEnum.STANDARD);
        dto.setBillingDate(BILLING_DATE);
        dto.setRatePerStudent(RATE_PER_STUDENT);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype TenantSubscriptionDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            SubscriptionCreateRequestDTO dto = buildDto();
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantSubscriptionDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            SubscriptionCreateRequestDTO dto = buildDto();
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantSubscriptionDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should unwrap maxUsers from JsonNullable when present")
        void shouldUnwrapMaxUsers_whenJsonNullableIsPresent() {
            // Given
            SubscriptionCreateRequestDTO dto = buildDto();
            dto.setMaxUsers(JsonNullable.of(MAX_USERS));
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantSubscriptionDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMaxUsers()).isEqualTo(MAX_USERS);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoMoreInteractions(applicationContext, modelMapper, repository);
        }

        @Test
        @DisplayName("Should leave maxUsers null when JsonNullable is undefined")
        void shouldLeaveMaxUsersNull_whenJsonNullableIsUndefined() {
            // Given — maxUsers defaults to JsonNullable.undefined() in the DTO
            SubscriptionCreateRequestDTO dto = buildDto();
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);

            // When
            TenantSubscriptionDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMaxUsers()).isNull();
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
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
            SubscriptionCreateRequestDTO dto = buildDto();
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel savedModel = new TenantSubscriptionDataModel();
            savedModel.setTenantSubscriptionId(SAVED_ID);
            TenantSubscriptionDTO expectedDto = new TenantSubscriptionDTO();
            expectedDto.setTenantSubscriptionId(SAVED_ID);

            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantSubscriptionDTO.class)).thenReturn(expectedDto);

            // When
            TenantSubscriptionDTO result = useCase.create(dto);

            // Then
            assertThat(result.getTenantSubscriptionId()).isEqualTo(SAVED_ID);
            InOrder inOrder = inOrder(applicationContext, modelMapper, repository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            inOrder.verify(repository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, TenantSubscriptionDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            SubscriptionCreateRequestDTO dto = buildDto();
            TenantSubscriptionDataModel prototypeModel = new TenantSubscriptionDataModel();
            TenantSubscriptionDataModel savedModel = new TenantSubscriptionDataModel();
            TenantSubscriptionDTO responseDto = new TenantSubscriptionDTO();

            when(applicationContext.getBean(TenantSubscriptionDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, TenantSubscriptionDTO.class)).thenReturn(responseDto);

            // When
            TenantSubscriptionDTO result = useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, repository);
            inOrder.verify(applicationContext, times(1)).getBean(TenantSubscriptionDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, TenantSubscriptionCreationUseCase.MAP_NAME);
            inOrder.verify(repository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, TenantSubscriptionDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
