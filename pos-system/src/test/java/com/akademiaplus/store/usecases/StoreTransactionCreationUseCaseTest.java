/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationResponseDTO;
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

@DisplayName("StoreTransactionCreationUseCase")
@ExtendWith(MockitoExtension.class)
class StoreTransactionCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private ModelMapper modelMapper;

    private StoreTransactionCreationUseCase useCase;

    private static final String TRANSACTION_TYPE = "SALE";
    private static final Double TOTAL_AMOUNT = 150.50;
    private static final String PAYMENT_METHOD = "CASH";
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new StoreTransactionCreationUseCase(
                applicationContext, storeTransactionRepository, modelMapper);
    }

    private StoreTransactionCreationRequestDTO buildDto() {
        StoreTransactionCreationRequestDTO dto = new StoreTransactionCreationRequestDTO();
        dto.setTransactionType(TRANSACTION_TYPE);
        dto.setTotalAmount(TOTAL_AMOUNT);
        dto.setPaymentMethod(PAYMENT_METHOD);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            StoreTransactionCreationRequestDTO dto = buildDto();
            StoreTransactionDataModel prototypeModel = new StoreTransactionDataModel();
            when(applicationContext.getBean(StoreTransactionDataModel.class)).thenReturn(prototypeModel);

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(StoreTransactionDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            StoreTransactionCreationRequestDTO dto = buildDto();
            StoreTransactionDataModel prototypeModel = new StoreTransactionDataModel();
            when(applicationContext.getBean(StoreTransactionDataModel.class)).thenReturn(prototypeModel);

            // When
            StoreTransactionDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, StoreTransactionCreationUseCase.MAP_NAME);
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
            StoreTransactionCreationRequestDTO dto = buildDto();
            StoreTransactionDataModel prototypeModel = new StoreTransactionDataModel();
            StoreTransactionDataModel savedModel = new StoreTransactionDataModel();
            savedModel.setStoreTransactionId(SAVED_ID);
            StoreTransactionCreationResponseDTO expectedDto = new StoreTransactionCreationResponseDTO();
            expectedDto.setStoreTransactionId(SAVED_ID);

            when(applicationContext.getBean(StoreTransactionDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreTransactionCreationUseCase.MAP_NAME);
            when(storeTransactionRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, StoreTransactionCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            StoreTransactionCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(storeTransactionRepository).saveAndFlush(prototypeModel);
            assertThat(result.getStoreTransactionId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            StoreTransactionCreationRequestDTO dto = buildDto();
            StoreTransactionDataModel prototypeModel = new StoreTransactionDataModel();
            StoreTransactionDataModel savedModel = new StoreTransactionDataModel();
            StoreTransactionCreationResponseDTO responseDto = new StoreTransactionCreationResponseDTO();

            when(applicationContext.getBean(StoreTransactionDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreTransactionCreationUseCase.MAP_NAME);
            when(storeTransactionRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, StoreTransactionCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, storeTransactionRepository);
            inOrder.verify(applicationContext).getBean(StoreTransactionDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, StoreTransactionCreationUseCase.MAP_NAME);
            inOrder.verify(storeTransactionRepository).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, StoreTransactionCreationResponseDTO.class);
        }
    }
}
