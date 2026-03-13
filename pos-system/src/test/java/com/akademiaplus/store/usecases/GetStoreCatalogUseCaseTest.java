/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetCatalogItemResponseDTO;
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

@DisplayName("GetStoreCatalogUseCase")
@ExtendWith(MockitoExtension.class)
class GetStoreCatalogUseCaseTest {

    private static final String CATEGORY = "Books";

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private ModelMapper modelMapper;

    private GetStoreCatalogUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStoreCatalogUseCase(storeProductRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no catalog products exist")
        void shouldReturnEmptyList_whenNoCatalogProductsExist() {
            // Given
            when(storeProductRepository.findCatalogProducts(null)).thenReturn(Collections.emptyList());

            // When
            List<GetCatalogItemResponseDTO> result = useCase.getCatalog(null);

            // Then
            assertThat(result).isEmpty();
            verify(storeProductRepository, times(1)).findCatalogProducts(null);
            verifyNoMoreInteractions(storeProductRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs with inStock set when products exist")
        void shouldReturnMappedDtosWithInStock_whenProductsExist() {
            // Given
            StoreProductDataModel product1 = new StoreProductDataModel();
            product1.setStockQuantity(10);
            StoreProductDataModel product2 = new StoreProductDataModel();
            product2.setStockQuantity(5);

            GetCatalogItemResponseDTO dto1 = new GetCatalogItemResponseDTO();
            GetCatalogItemResponseDTO dto2 = new GetCatalogItemResponseDTO();

            when(storeProductRepository.findCatalogProducts(null))
                    .thenReturn(List.of(product1, product2));
            when(modelMapper.map(product1, GetCatalogItemResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(product2, GetCatalogItemResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetCatalogItemResponseDTO> result = useCase.getCatalog(null);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            assertThat(dto1.getInStock()).isTrue();
            assertThat(dto2.getInStock()).isTrue();
            InOrder inOrder = inOrder(storeProductRepository, modelMapper);
            inOrder.verify(storeProductRepository, times(1)).findCatalogProducts(null);
            inOrder.verify(modelMapper, times(1)).map(product1, GetCatalogItemResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(product2, GetCatalogItemResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should pass category filter to repository")
        void shouldPassCategoryFilter_whenCategoryProvided() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            product.setStockQuantity(3);
            GetCatalogItemResponseDTO dto = new GetCatalogItemResponseDTO();

            when(storeProductRepository.findCatalogProducts(CATEGORY))
                    .thenReturn(List.of(product));
            when(modelMapper.map(product, GetCatalogItemResponseDTO.class)).thenReturn(dto);

            // When
            List<GetCatalogItemResponseDTO> result = useCase.getCatalog(CATEGORY);

            // Then
            assertThat(result).containsExactly(dto);
            verify(storeProductRepository, times(1)).findCatalogProducts(CATEGORY);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository findCatalogProducts throws")
        void shouldPropagateException_whenRepositoryFindCatalogProductsThrows() {
            // Given
            when(storeProductRepository.findCatalogProducts(null))
                    .thenThrow(new RuntimeException("DB connection failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getCatalog(null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection failed");

            verify(storeProductRepository, times(1)).findCatalogProducts(null);
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            product.setStockQuantity(10);
            when(storeProductRepository.findCatalogProducts(null))
                    .thenReturn(List.of(product));
            when(modelMapper.map(product, GetCatalogItemResponseDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getCatalog(null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping failed");

            verify(storeProductRepository, times(1)).findCatalogProducts(null);
            verify(modelMapper, times(1)).map(product, GetCatalogItemResponseDTO.class);
        }
    }
}
