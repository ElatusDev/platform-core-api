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
import com.akademiaplus.payment.usecases.GetAllPaymentAdultStudentsUseCase;
import com.akademiaplus.payment.usecases.GetPaymentAdultStudentByIdUseCase;
import com.akademiaplus.payment.usecases.PaymentAdultStudentCreationUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetPaymentAdultStudentResponseDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationResponseDTO;
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

@DisplayName("PaymentAdultStudentController")
@ExtendWith(MockitoExtension.class)
class PaymentAdultStudentControllerTest {

    private static final Long PAYMENT_ADULT_STUDENT_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/paymentAdultStudents";

    @Mock private PaymentAdultStudentCreationUseCase creationUseCase;
    @Mock private GetAllPaymentAdultStudentsUseCase getAllUseCase;
    @Mock private GetPaymentAdultStudentByIdUseCase getByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PaymentAdultStudentController controller = new PaymentAdultStudentController(
                creationUseCase, getAllUseCase, getByIdUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/paymentAdultStudents")
    class CreatePaymentAdultStudent {
        @Test
        @DisplayName("Should return 201 when payment-adult-student created successfully")
        void shouldReturn201_whenPaymentAdultStudentCreatedSuccessfully() throws Exception {
            // Given
            PaymentAdultStudentCreationRequestDTO request = new PaymentAdultStudentCreationRequestDTO();
            request.setPaymentDate(LocalDate.of(2026, 2, 1));
            request.setAmount(99.99);
            request.setPaymentMethod("CREDIT_CARD");
            request.setMembershipAdultStudentId(1L);

            PaymentAdultStudentCreationResponseDTO response = new PaymentAdultStudentCreationResponseDTO();
            response.setPaymentAdultStudentId(PAYMENT_ADULT_STUDENT_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.paymentAdultStudentId").value(PAYMENT_ADULT_STUDENT_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/paymentAdultStudents")
    class GetAllPaymentAdultStudents {
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
            GetPaymentAdultStudentResponseDTO dto1 = new GetPaymentAdultStudentResponseDTO();
            dto1.setPaymentAdultStudentId(100L);

            GetPaymentAdultStudentResponseDTO dto2 = new GetPaymentAdultStudentResponseDTO();
            dto2.setPaymentAdultStudentId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].paymentAdultStudentId").value(100L))
                    .andExpect(jsonPath("$[1].paymentAdultStudentId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/paymentAdultStudents/{id}")
    class GetPaymentAdultStudentById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetPaymentAdultStudentResponseDTO dto = new GetPaymentAdultStudentResponseDTO();
            dto.setPaymentAdultStudentId(PAYMENT_ADULT_STUDENT_ID);

            when(getByIdUseCase.get(PAYMENT_ADULT_STUDENT_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + PAYMENT_ADULT_STUDENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paymentAdultStudentId").value(PAYMENT_ADULT_STUDENT_ID));

            verify(getByIdUseCase).get(PAYMENT_ADULT_STUDENT_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(PAYMENT_ADULT_STUDENT_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.PAYMENT_ADULT_STUDENT, String.valueOf(PAYMENT_ADULT_STUDENT_ID)));
            when(messageService.getEntityNotFound(EntityType.PAYMENT_ADULT_STUDENT, String.valueOf(PAYMENT_ADULT_STUDENT_ID)))
                    .thenReturn("PaymentAdultStudent not found: " + PAYMENT_ADULT_STUDENT_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + PAYMENT_ADULT_STUDENT_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(PAYMENT_ADULT_STUDENT_ID);
            verify(messageService).getEntityNotFound(EntityType.PAYMENT_ADULT_STUDENT, String.valueOf(PAYMENT_ADULT_STUDENT_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
