/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.config.PosControllerAdvice;
import com.akademiaplus.exception.StoreProductNotFoundException;
import com.akademiaplus.store.usecases.GetAllStoreProductsUseCase;
import com.akademiaplus.store.usecases.GetStoreProductByIdUseCase;
import com.akademiaplus.store.usecases.StoreProductCreationUseCase;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
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

@DisplayName("StoreProductController")
@ExtendWith(MockitoExtension.class)
class StoreProductControllerTest {

    private static final Long STORE_PRODUCT_ID = 100L;
    private static final String BASE_PATH = "/v1/pos-system/store-products";

    @Mock private StoreProductCreationUseCase storeProductCreationUseCase;
    @Mock private GetAllStoreProductsUseCase getAllStoreProductsUseCase;
    @Mock private GetStoreProductByIdUseCase getStoreProductByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StoreProductController controller = new StoreProductController(
                storeProductCreationUseCase, getAllStoreProductsUseCase, getStoreProductByIdUseCase);
        PosControllerAdvice controllerAdvice = new PosControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /store-products")
    class GetAllStoreProducts {

        @Test
        @DisplayName("Should return 200 with empty list when no store products exist")
        void shouldReturn200WithEmptyList_whenNoStoreProductsExist() throws Exception {
            // Given
            when(getAllStoreProductsUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllStoreProductsUseCase).getAll();
            verifyNoMoreInteractions(getAllStoreProductsUseCase);
        }

        @Test
        @DisplayName("Should return 200 with store product list when store products exist")
        void shouldReturn200WithStoreProductList_whenStoreProductsExist() throws Exception {
            // Given
            GetStoreProductResponseDTO dto1 = new GetStoreProductResponseDTO();
            GetStoreProductResponseDTO dto2 = new GetStoreProductResponseDTO();
            when(getAllStoreProductsUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllStoreProductsUseCase).getAll();
            verifyNoMoreInteractions(getAllStoreProductsUseCase);
        }
    }

    @Nested
    @DisplayName("GET /store-products/{storeProductId}")
    class GetStoreProductById {

        @Test
        @DisplayName("Should return 200 with store product when found")
        void shouldReturn200WithStoreProduct_whenFound() throws Exception {
            // Given
            GetStoreProductResponseDTO dto = new GetStoreProductResponseDTO();
            when(getStoreProductByIdUseCase.get(STORE_PRODUCT_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{storeProductId}", STORE_PRODUCT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getStoreProductByIdUseCase).get(STORE_PRODUCT_ID);
            verifyNoMoreInteractions(getStoreProductByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when store product not found")
        void shouldReturn404_whenStoreProductNotFound() throws Exception {
            // Given
            when(getStoreProductByIdUseCase.get(STORE_PRODUCT_ID))
                    .thenThrow(new StoreProductNotFoundException(String.valueOf(STORE_PRODUCT_ID)));
            when(messageService.getStoreProductNotFound(String.valueOf(STORE_PRODUCT_ID)))
                    .thenReturn("Store product not found: " + STORE_PRODUCT_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{storeProductId}", STORE_PRODUCT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getStoreProductByIdUseCase).get(STORE_PRODUCT_ID);
            verify(messageService).getStoreProductNotFound(String.valueOf(STORE_PRODUCT_ID));
            verifyNoMoreInteractions(getStoreProductByIdUseCase, messageService);
        }
    }
}
