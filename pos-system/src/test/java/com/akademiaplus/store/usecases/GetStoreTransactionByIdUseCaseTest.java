/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.exception.StoreTransactionNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
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

@DisplayName("GetStoreTransactionByIdUseCase")
@ExtendWith(MockitoExtension.class)
class GetStoreTransactionByIdUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long STORE_TRANSACTION_ID = 100L;

    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private GetStoreTransactionByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetStoreTransactionByIdUseCase(storeTransactionRepository, tenantContextHolder, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return mapped DTO when store transaction is found")
        void shouldReturnMappedDto_whenStoreTransactionFound() {
            // Given
            StoreTransactionDataModel transaction = new StoreTransactionDataModel();
            GetStoreTransactionResponseDTO expectedDto = new GetStoreTransactionResponseDTO();

            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeTransactionRepository.findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID)))
                    .thenReturn(Optional.of(transaction));
            when(modelMapper.map(transaction, GetStoreTransactionResponseDTO.class)).thenReturn(expectedDto);

            // When
            GetStoreTransactionResponseDTO result = useCase.get(STORE_TRANSACTION_ID);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(tenantContextHolder).getTenantId();
            verify(storeTransactionRepository).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            verify(modelMapper).map(transaction, GetStoreTransactionResponseDTO.class);
            verifyNoMoreInteractions(tenantContextHolder, storeTransactionRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw StoreTransactionNotFoundException when store transaction not found")
        void shouldThrowStoreTransactionNotFoundException_whenStoreTransactionNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeTransactionRepository.findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_TRANSACTION_ID))
                    .isInstanceOf(StoreTransactionNotFoundException.class)
                    .hasMessage(String.valueOf(STORE_TRANSACTION_ID));
            verify(tenantContextHolder).getTenantId();
            verify(storeTransactionRepository).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            verifyNoMoreInteractions(tenantContextHolder, storeTransactionRepository, modelMapper);
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
            assertThatThrownBy(() -> useCase.get(STORE_TRANSACTION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(GetStoreTransactionByIdUseCase.ERROR_TENANT_CONTEXT_REQUIRED);
            verify(tenantContextHolder).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder, storeTransactionRepository, modelMapper);
        }
    }
}
