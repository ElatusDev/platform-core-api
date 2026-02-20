/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.exception.StoreProductNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
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

@DisplayName("GetStoreProductByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetStoreProductByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long STORE_PRODUCT_ID = 100L;

    @Mock private StoreProductRepository storeProductRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetStoreProductByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStoreProductByIdUseCase(storeProductRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when store product is found")
        void shouldReturnMappedDto_whenStoreProductFound() {
            // Given
            StoreProductDataModel product = new StoreProductDataModel();
            GetStoreProductResponseDTO expectedDto = new GetStoreProductResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeProductRepository.findById(
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID)))
                    .thenReturn(Optional.of(product));
            when(modelMapper.map(product, GetStoreProductResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetStoreProductResponseDTO result = useCase.get(STORE_PRODUCT_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(storeProductRepository).findById(
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID));
            verify(modelMapper).map(product, GetStoreProductResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, storeProductRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw StoreProductNotFoundException when store product not found")
        void shouldThrowStoreProductNotFoundException_whenStoreProductNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeProductRepository.findById(
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_PRODUCT_ID))
                    .isInstanceOf(StoreProductNotFoundException.class)
                    .hasMessage(String.valueOf(STORE_PRODUCT_ID));
            verify(tenantContextHolder).getTenantId();
            verify(storeProductRepository).findById(
                    new StoreProductDataModel.ProductCompositeId(TENANT_ID, STORE_PRODUCT_ID));
            verifyNoMoreInteractions(tenantContextHolder, storeProductRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Tenant context")
    class TenantContext {

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_PRODUCT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetStoreProductByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, storeProductRepository, modelMapper);
        }
    }
}
