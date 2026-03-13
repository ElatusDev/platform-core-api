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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetAllStoreTransactionsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllStoreTransactionsUseCaseTest {

    @Mock private StoreTransactionRepository storeTransactionRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllStoreTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllStoreTransactionsUseCase(storeTransactionRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no store transactions exist")
        void shouldReturnEmptyList_whenNoStoreTransactionsExist() {
            // Given
            when(storeTransactionRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetStoreTransactionResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(storeTransactionRepository, times(1)).findAll();
            verifyNoMoreInteractions(storeTransactionRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when store transactions exist")
        void shouldReturnMappedDtos_whenStoreTransactionsExist() {
            // Given
            StoreTransactionDataModel transaction1 = new StoreTransactionDataModel();
            StoreTransactionDataModel transaction2 = new StoreTransactionDataModel();
            GetStoreTransactionResponseDTO dto1 = new GetStoreTransactionResponseDTO();
            GetStoreTransactionResponseDTO dto2 = new GetStoreTransactionResponseDTO();

            when(storeTransactionRepository.findAll()).thenReturn(List.of(transaction1, transaction2));
            when(modelMapper.map(transaction1, GetStoreTransactionResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(transaction2, GetStoreTransactionResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetStoreTransactionResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(storeTransactionRepository, modelMapper);
            inOrder.verify(storeTransactionRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(transaction1, GetStoreTransactionResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(transaction2, GetStoreTransactionResponseDTO.class);
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
            when(storeTransactionRepository.findAll())
                    .thenThrow(new RuntimeException("DB connection failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection failed");

            verify(storeTransactionRepository, times(1)).findAll();
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("Should propagate exception when modelMapper throws")
        void shouldPropagateException_whenModelMapperThrows() {
            // Given
            StoreTransactionDataModel transaction = new StoreTransactionDataModel();
            when(storeTransactionRepository.findAll()).thenReturn(List.of(transaction));
            when(modelMapper.map(transaction, GetStoreTransactionResponseDTO.class))
                    .thenThrow(new RuntimeException("Mapping failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.getAll())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mapping failed");

            verify(storeTransactionRepository, times(1)).findAll();
            verify(modelMapper, times(1)).map(transaction, GetStoreTransactionResponseDTO.class);
        }
    }
}
