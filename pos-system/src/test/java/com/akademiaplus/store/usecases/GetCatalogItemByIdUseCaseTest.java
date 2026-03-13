/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetCatalogItemByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetCatalogItemByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long STORE_PRODUCT_ID = 100L;

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetCatalogItemByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCatalogItemByIdUseCase(storeProductRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO with inStock true when product has stock")
        void shouldReturnMappedDtoWithInStockTrue_whenProductHasStock() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            product.setStockQuantity(10);
            GetCatalogItemResponseDTO expectedDto = new GetCatalogItemResponseDTO();

            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.of(product));
            when(modelMapper.map(product, GetCatalogItemResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetCatalogItemResponseDTO result = useCase.get(STORE_PRODUCT_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getInStock()).isTrue();
            InOrder inOrder = inOrder(tenantContextHolder, storeProductRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(storeProductRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(product, GetCatalogItemResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return mapped DTO with inStock false when product has no stock")
        void shouldReturnMappedDtoWithInStockFalse_whenProductHasNoStock() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            product.setStockQuantity(0);
            GetCatalogItemResponseDTO expectedDto = new GetCatalogItemResponseDTO();

            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.of(product));
            when(modelMapper.map(product, GetCatalogItemResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetCatalogItemResponseDTO result = useCase.get(STORE_PRODUCT_ID);

            // Then
            assertThat(result.getInStock()).isFalse();
        }
    }

    @Nested
    @DisplayName("Not Found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when product not found")
        void shouldThrowEntityNotFoundException_whenProductNotFound() {
            // Given
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_PRODUCT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasFieldOrPropertyWithValue("entityType", EntityType.STORE_PRODUCT)
                    .hasFieldOrPropertyWithValue("entityId", String.valueOf(STORE_PRODUCT_ID));

            InOrder inOrder = inOrder(tenantContextHolder, storeProductRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(storeProductRepository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(modelMapper);
        }
    }
}
