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
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetAllStoreProductsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllStoreProductsUseCaseTest {

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllStoreProductsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllStoreProductsUseCase(storeProductRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no store products exist")
        void shouldReturnEmptyList_whenNoStoreProductsExist() {
            // Given
            when(storeProductRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetStoreProductResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(storeProductRepository, times(1)).findAll();
            verifyNoMoreInteractions(storeProductRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when store products exist")
        void shouldReturnMappedDtos_whenStoreProductsExist() {
            // Given
            StoreProductDataModel product1 = new StoreProductDataModel();
            StoreProductDataModel product2 = new StoreProductDataModel();
            GetStoreProductResponseDTO dto1 = new GetStoreProductResponseDTO();
            GetStoreProductResponseDTO dto2 = new GetStoreProductResponseDTO();

            when(storeProductRepository.findAll()).thenReturn(List.of(product1, product2));
            when(modelMapper.map(product1, GetStoreProductResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(product2, GetStoreProductResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetStoreProductResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(storeProductRepository, modelMapper);
            inOrder.verify(storeProductRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(product1, GetStoreProductResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(product2, GetStoreProductResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository findAll throws")
        void shouldPropagateException_whenRepositoryFindAllThrows() {
            // Given
            when(storeProductRepository.findAll())
                    .thenThrow(new RuntimeException("DB connection failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection failed");

            verify(storeProductRepository, times(1)).findAll();
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            when(storeProductRepository.findAll()).thenReturn(List.of(product));
            when(modelMapper.map(product, GetStoreProductResponseDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping failed");

            verify(storeProductRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(product, GetStoreProductResponseDTO.class);
        }
    }
}
