/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.interfaceadapters;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akademiaplus.config.BillingControllerAdvice;
import com.akademiaplus.exception.CompensationNotFoundException;
import com.akademiaplus.payroll.usecases.CompensationCreationUseCase;
import com.akademiaplus.payroll.usecases.GetAllCompensationsUseCase;
import com.akademiaplus.payroll.usecases.GetCompensationByIdUseCase;
import com.akademiaplus.utilities.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationResponseDTO;
import openapi.akademiaplus.domain.billing.dto.GetCompensationResponseDTO;
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

@DisplayName("CompensationController")
@ExtendWith(MockitoExtension.class)
class CompensationControllerTest {

    private static final Long COMPENSATION_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/compensations";

    @Mock private CompensationCreationUseCase creationUseCase;
    @Mock private GetAllCompensationsUseCase getAllUseCase;
    @Mock private GetCompensationByIdUseCase getByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CompensationController controller = new CompensationController(
                creationUseCase, getAllUseCase, getByIdUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/compensations")
    class CreateCompensation {
        @Test
        @DisplayName("Should return 201 when compensation created successfully")
        void shouldReturn201_whenCompensationCreatedSuccessfully() throws Exception {
            // Given
            CompensationCreationRequestDTO request = new CompensationCreationRequestDTO();
            request.setCompensationType("SALARY");
            request.setAmount(1500.00);

            CompensationCreationResponseDTO response = new CompensationCreationResponseDTO();
            response.setCompensationId(COMPENSATION_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.compensationId").value(COMPENSATION_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/compensations")
    class GetAllCompensations {
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
            GetCompensationResponseDTO dto1 = new GetCompensationResponseDTO();
            dto1.setCompensationId(100L);

            GetCompensationResponseDTO dto2 = new GetCompensationResponseDTO();
            dto2.setCompensationId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].compensationId").value(100L))
                    .andExpect(jsonPath("$[1].compensationId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/compensations/{id}")
    class GetCompensationById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetCompensationResponseDTO dto = new GetCompensationResponseDTO();
            dto.setCompensationId(COMPENSATION_ID);

            when(getByIdUseCase.get(COMPENSATION_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + COMPENSATION_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.compensationId").value(COMPENSATION_ID));

            verify(getByIdUseCase).get(COMPENSATION_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(COMPENSATION_ID))
                    .thenThrow(new CompensationNotFoundException(String.valueOf(COMPENSATION_ID)));
            when(messageService.getCompensationNotFound(String.valueOf(COMPENSATION_ID)))
                    .thenReturn("Compensation not found: " + COMPENSATION_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + COMPENSATION_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(COMPENSATION_ID);
            verify(messageService).getCompensationNotFound(String.valueOf(COMPENSATION_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
