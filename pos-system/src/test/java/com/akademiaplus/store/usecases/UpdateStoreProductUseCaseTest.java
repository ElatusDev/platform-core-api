/*
 * Copyright (c) 2025 ElatusDev
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
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UpdateStoreProductUseCase}.
 */
@DisplayName("UpdateStoreProductUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateStoreProductUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PRODUCT_ID = 42L;
    private static final String PRODUCT_NAME = "Notebook";
    private static final Double PRODUCT_PRICE = 9.99;
    private static final Integer STOCK_QUANTITY = 100;

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private UpdateStoreProductUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateStoreProductUseCase(
                storeProductRepository, tenantContextHolder, modelMapper);
    }

    private StoreProductCreationRequestDTO buildDto() {
        StoreProductCreationRequestDTO dto = new StoreProductCreationRequestDTO();
        dto.setName(PRODUCT_NAME);
        dto.setPrice(PRODUCT_PRICE);
        dto.setStockQuantity(STOCK_QUANTITY);
        return dto;
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should find existing product and map DTO onto it")
        void shouldFindAndMapDto_whenProductExists() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel existing = new StoreProductDataModel();
            StoreProductDataModel saved = new StoreProductDataModel();
            StoreProductCreationResponseDTO expectedResponse = new StoreProductCreationResponseDTO();
            expectedResponse.setStoreProductId(PRODUCT_ID);

            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, UpdateStoreProductUseCase.MAP_NAME);
            when(storeProductRepository.saveAndFlush(existing)).thenReturn(saved);
            when(modelMapper.map(saved, StoreProductCreationResponseDTO.class))
                    .thenReturn(expectedResponse);

            // When
            StoreProductCreationResponseDTO result = useCase.update(PRODUCT_ID, dto);

            // Then
            verify(storeProductRepository).findById(compositeId);
            verify(modelMapper).map(dto, existing, UpdateStoreProductUseCase.MAP_NAME);
            verify(storeProductRepository).saveAndFlush(existing);
            assertThat(result.getStoreProductId()).isEqualTo(PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("Product Not Found")
    class ProductNotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when product does not exist")
        void shouldThrowEntityNotFound_whenProductDoesNotExist() {
            // Given
            StoreProductCreationRequestDTO dto = buildDto();
            StoreProductDataModel.ProductCompositeId compositeId =
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, PRODUCT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(storeProductRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(PRODUCT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.STORE_PRODUCT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(PRODUCT_ID));
                    });
        }
    }
}
