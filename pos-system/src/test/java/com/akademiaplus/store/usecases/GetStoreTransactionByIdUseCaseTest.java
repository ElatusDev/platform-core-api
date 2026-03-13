/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
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
            InOrder inOrder = inOrder(tenantContextHolder, storeTransactionRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(storeTransactionRepository, times(1)).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            inOrder.verify(modelMapper, times(1)).map(transaction, GetStoreTransactionResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Not found")
    class NotFound {

        @Test
        @DisplayName("Should throw EntityNotFoundException when store transaction not found")
        void shouldThrowEntityNotFoundException_whenStoreTransactionNotFound() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeTransactionRepository.findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_TRANSACTION_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.STORE_TRANSACTION, STORE_TRANSACTION_ID));

            InOrder inOrder = inOrder(tenantContextHolder, storeTransactionRepository);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(storeTransactionRepository, times(1)).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(modelMapper);
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

            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(tenantContextHolder);
            verifyNoInteractions(storeTransactionRepository, modelMapper);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository findById throws")
        void shouldPropagateException_whenRepositoryFindByIdThrows() {
            // Given
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeTransactionRepository.findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID)))
                    .thenThrow(new RuntimeException("DB connection failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_TRANSACTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection failed");

            verify(tenantContextHolder, times(1)).getTenantId();
            verify(storeTransactionRepository, times(1)).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            StoreTransactionDataModel transaction = new StoreTransactionDataModel();
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
            when(storeTransactionRepository.findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID)))
                    .thenReturn(Optional.of(transaction));
            when(modelMapper.map(transaction, GetStoreTransactionResponseDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.get(STORE_TRANSACTION_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping failed");

            InOrder inOrder = inOrder(tenantContextHolder, storeTransactionRepository, modelMapper);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(storeTransactionRepository, times(1)).findById(
                    new StoreTransactionDataModel.StoreTransactionCompositeId(TENANT_ID, STORE_TRANSACTION_ID));
            inOrder.verify(modelMapper, times(1)).map(transaction, GetStoreTransactionResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
