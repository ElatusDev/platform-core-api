/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("StoreProductCreationUseCase")
@ExtendWith(MockitoExtension.class)
class StoreProductCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ModelMapper modelMapper;

    private StoreProductCreationUseCase useCase;

    private static final String PRODUCT_NAME = "Math Textbook";
    private static final String PRODUCT_DESCRIPTION = "Algebra fundamentals";
    private static final Double PRICE = 29.99;
    private static final Integer STOCK_QUANTITY = 100;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new StoreProductCreationUseCase(
                applicationContext, storeProductRepository, modelMapper);
    }

    private StoreProductCreationRequestDTO buildDto() {
        StoreProductCreationRequestDTO dto = new StoreProductCreationRequestDTO();
        dto.setName(PRODUCT_NAME);
        dto.setDescription(PRODUCT_DESCRIPTION);
        dto.setPrice(PRICE);
        dto.setStockQuantity(STOCK_QUANTITY);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);

            // When
            StoreProductDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(storeProductRepository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);

            // When
            StoreProductDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(storeProductRepository);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            StoreProductDataModel savedModel = new StoreProductDataModel();
            savedModel.setStoreProductId(SAVED_ID);
            StoreProductCreationResponseDTO expectedDto = new StoreProductCreationResponseDTO();
            expectedDto.setStoreProductId(SAVED_ID);

            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, StoreProductCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            StoreProductCreationResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result.getStoreProductId()).isEqualTo(SAVED_ID);
            InOrder inOrder = inOrder(applicationContext, modelMapper, storeProductRepository);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verify(storeProductRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, StoreProductCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            StoreProductDataModel savedModel = new StoreProductDataModel();
            StoreProductCreationResponseDTO responseDto = new StoreProductCreationResponseDTO();

            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, StoreProductCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            StoreProductCreationResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, storeProductRepository);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verify(storeProductRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, StoreProductCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when applicationContext getBean throws")
        void shouldPropagateException_whenApplicationContextGetBeanThrows() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            when(applicationContext.getBean(StoreProductDataModel.class))
                    .thenThrow(new RuntimeException("Bean creation failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Bean creation failed");

            verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            verifyNoInteractions(storeProductRepository);
        }

        @Test
        @DisplayName("Should propagate exception when repository saveAndFlush throws")
        void shouldPropagateException_whenRepositorySaveAndFlushThrows() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(prototypeModel))
                    .thenThrow(new RuntimeException("DB write failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB write failed");

            InOrder inOrder = inOrder(applicationContext, modelMapper, storeProductRepository);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verify(storeProductRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper response mapping throws")
        void shouldPropagateException_whenModelMapperResponseMappingThrows() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel prototypeModel = new StoreProductDataModel();
            StoreProductDataModel savedModel = new StoreProductDataModel();
            when(applicationContext.getBean(StoreProductDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, StoreProductCreationResponseDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping failed");

            InOrder inOrder = inOrder(applicationContext, modelMapper, storeProductRepository);
            inOrder.verify(applicationContext, times(1)).getBean(StoreProductDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, StoreProductCreationUseCase.MAP_NAME);
            inOrder.verify(storeProductRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, StoreProductCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
