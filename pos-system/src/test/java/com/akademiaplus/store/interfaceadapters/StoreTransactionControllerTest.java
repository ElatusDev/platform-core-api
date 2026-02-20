/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.config.PosControllerAdvice;
import com.akademiaplus.exception.StoreTransactionNotFoundException;
import com.akademiaplus.store.usecases.GetAllStoreTransactionsUseCase;
import com.akademiaplus.store.usecases.GetStoreTransactionByIdUseCase;
import com.akademiaplus.store.usecases.StoreTransactionCreationUseCase;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("StoreTransactionController")
@ExtendWith(MockitoExtension.class)
class StoreTransactionControllerTest {

    private static final Long STORE_TRANSACTION_ID = 100L;
    private static final String BASE_PATH = "/v1/pos-system/store-transactions";

    @Mock private StoreTransactionCreationUseCase storeTransactionCreationUseCase;
    @Mock private GetAllStoreTransactionsUseCase getAllStoreTransactionsUseCase;
    @Mock private GetStoreTransactionByIdUseCase getStoreTransactionByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StoreTransactionController controller = new StoreTransactionController(
                storeTransactionCreationUseCase, getAllStoreTransactionsUseCase, getStoreTransactionByIdUseCase);
        PosControllerAdvice controllerAdvice = new PosControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /store-transactions")
    class GetAllStoreTransactions {

        @Test
        @DisplayName("Should return 200 with empty list when no store transactions exist")
        void shouldReturn200WithEmptyList_whenNoStoreTransactionsExist() throws Exception {
            // Given
            when(getAllStoreTransactionsUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllStoreTransactionsUseCase).getAll();
            verifyNoMoreInteractions(getAllStoreTransactionsUseCase);
        }

        @Test
        @DisplayName("Should return 200 with store transaction list when store transactions exist")
        void shouldReturn200WithStoreTransactionList_whenStoreTransactionsExist() throws Exception {
            // Given
            GetStoreTransactionResponseDTO dto1 = new GetStoreTransactionResponseDTO();
            GetStoreTransactionResponseDTO dto2 = new GetStoreTransactionResponseDTO();
            when(getAllStoreTransactionsUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllStoreTransactionsUseCase).getAll();
            verifyNoMoreInteractions(getAllStoreTransactionsUseCase);
        }
    }

    @Nested
    @DisplayName("GET /store-transactions/{storeTransactionId}")
    class GetStoreTransactionById {

        @Test
        @DisplayName("Should return 200 with store transaction when found")
        void shouldReturn200WithStoreTransaction_whenFound() throws Exception {
            // Given
            GetStoreTransactionResponseDTO dto = new GetStoreTransactionResponseDTO();
            when(getStoreTransactionByIdUseCase.get(STORE_TRANSACTION_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{storeTransactionId}", STORE_TRANSACTION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getStoreTransactionByIdUseCase).get(STORE_TRANSACTION_ID);
            verifyNoMoreInteractions(getStoreTransactionByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when store transaction not found")
        void shouldReturn404_whenStoreTransactionNotFound() throws Exception {
            // Given
            when(getStoreTransactionByIdUseCase.get(STORE_TRANSACTION_ID))
                    .thenThrow(new StoreTransactionNotFoundException(String.valueOf(STORE_TRANSACTION_ID)));
            when(messageService.getStoreTransactionNotFound(String.valueOf(STORE_TRANSACTION_ID)))
                    .thenReturn("Store transaction not found: " + STORE_TRANSACTION_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{storeTransactionId}", STORE_TRANSACTION_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getStoreTransactionByIdUseCase).get(STORE_TRANSACTION_ID);
            verify(messageService).getStoreTransactionNotFound(String.valueOf(STORE_TRANSACTION_ID));
            verifyNoMoreInteractions(getStoreTransactionByIdUseCase, messageService);
        }
    }
}
