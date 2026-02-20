/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.interfaceadapters;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akademiaplus.config.BillingControllerAdvice;
import com.akademiaplus.payment.usecases.GetAllPaymentTutorsUseCase;
import com.akademiaplus.payment.usecases.GetPaymentTutorByIdUseCase;
import com.akademiaplus.payment.usecases.PaymentTutorCreationUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetPaymentTutorResponseDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationResponseDTO;
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

@DisplayName("PaymentTutorController")
@ExtendWith(MockitoExtension.class)
class PaymentTutorControllerTest {

    private static final Long PAYMENT_TUTOR_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/paymentTutors";

    @Mock private PaymentTutorCreationUseCase creationUseCase;
    @Mock private GetAllPaymentTutorsUseCase getAllUseCase;
    @Mock private GetPaymentTutorByIdUseCase getByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PaymentTutorController controller = new PaymentTutorController(
                creationUseCase, getAllUseCase, getByIdUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/paymentTutors")
    class CreatePaymentTutor {
        @Test
        @DisplayName("Should return 201 when payment-tutor created successfully")
        void shouldReturn201_whenPaymentTutorCreatedSuccessfully() throws Exception {
            // Given
            PaymentTutorCreationRequestDTO request = new PaymentTutorCreationRequestDTO();
            request.setPaymentDate(LocalDate.of(2026, 2, 1));
            request.setAmount(150.00);
            request.setPaymentMethod("BANK_TRANSFER");
            request.setMembershipTutorId(1L);

            PaymentTutorCreationResponseDTO response = new PaymentTutorCreationResponseDTO();
            response.setPaymentTutorId(PAYMENT_TUTOR_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentTutorId").value(PAYMENT_TUTOR_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/paymentTutors")
    class GetAllPaymentTutors {
        @Test
        @DisplayName("Should return 200 with empty list when none exist")
        void shouldReturn200WithEmptyList_whenNoneExist() throws Exception {
            // Given
            when(getAllUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 200 with list when entities exist")
        void shouldReturn200WithList_whenEntitiesExist() throws Exception {
            // Given
            GetPaymentTutorResponseDTO dto1 = new GetPaymentTutorResponseDTO();
            dto1.setPaymentTutorId(100L);

            GetPaymentTutorResponseDTO dto2 = new GetPaymentTutorResponseDTO();
            dto2.setPaymentTutorId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].paymentTutorId").value(100L))
                    .andExpect(jsonPath("$[1].paymentTutorId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/paymentTutors/{id}")
    class GetPaymentTutorById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetPaymentTutorResponseDTO dto = new GetPaymentTutorResponseDTO();
            dto.setPaymentTutorId(PAYMENT_TUTOR_ID);

            when(getByIdUseCase.get(PAYMENT_TUTOR_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + PAYMENT_TUTOR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentTutorId").value(PAYMENT_TUTOR_ID));

            verify(getByIdUseCase).get(PAYMENT_TUTOR_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(PAYMENT_TUTOR_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.PAYMENT_TUTOR, String.valueOf(PAYMENT_TUTOR_ID)));
            when(messageService.getEntityNotFound(EntityType.PAYMENT_TUTOR, String.valueOf(PAYMENT_TUTOR_ID)))
                    .thenReturn("PaymentTutor not found: " + PAYMENT_TUTOR_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + PAYMENT_TUTOR_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(PAYMENT_TUTOR_ID);
            verify(messageService).getEntityNotFound(EntityType.PAYMENT_TUTOR, String.valueOf(PAYMENT_TUTOR_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
