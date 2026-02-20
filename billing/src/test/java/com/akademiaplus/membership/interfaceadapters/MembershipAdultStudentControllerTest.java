/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.akademiaplus.config.BillingControllerAdvice;
import com.akademiaplus.exception.MembershipAdultStudentNotFoundException;
import com.akademiaplus.membership.usecases.GetAllMembershipAdultStudentsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipAdultStudentByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipAdultStudentCreationUseCase;
import com.akademiaplus.utilities.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationResponseDTO;
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

@DisplayName("MembershipAdultStudentController")
@ExtendWith(MockitoExtension.class)
class MembershipAdultStudentControllerTest {

    private static final Long MEMBERSHIP_ADULT_STUDENT_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/membershipAdultStudents";

    @Mock private MembershipAdultStudentCreationUseCase creationUseCase;
    @Mock private GetAllMembershipAdultStudentsUseCase getAllUseCase;
    @Mock private GetMembershipAdultStudentByIdUseCase getByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MembershipAdultStudentController controller = new MembershipAdultStudentController(
                creationUseCase, getAllUseCase, getByIdUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/membershipAdultStudents")
    class CreateMembershipAdultStudent {
        @Test
        @DisplayName("Should return 201 when membership-adult-student created successfully")
        void shouldReturn201_whenMembershipAdultStudentCreatedSuccessfully() throws Exception {
            // Given
            MembershipAdultStudentCreationRequestDTO request = new MembershipAdultStudentCreationRequestDTO();
            request.setStartDate(LocalDate.of(2026, 1, 1));
            request.setDueDate(LocalDate.of(2026, 2, 1));
            request.setMembershipId(1L);
            request.setCourseId(2L);
            request.setAdultStudentId(3L);

            MembershipAdultStudentCreationResponseDTO response = new MembershipAdultStudentCreationResponseDTO();
            response.setMembershipAdultStudentId(MEMBERSHIP_ADULT_STUDENT_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.membershipAdultStudentId").value(MEMBERSHIP_ADULT_STUDENT_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/membershipAdultStudents")
    class GetAllMembershipAdultStudents {
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
            GetMembershipAdultStudentResponseDTO dto1 = new GetMembershipAdultStudentResponseDTO();
            dto1.setMembershipAdultStudentId(100L);

            GetMembershipAdultStudentResponseDTO dto2 = new GetMembershipAdultStudentResponseDTO();
            dto2.setMembershipAdultStudentId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].membershipAdultStudentId").value(100L))
                    .andExpect(jsonPath("$[1].membershipAdultStudentId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/membershipAdultStudents/{id}")
    class GetMembershipAdultStudentById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetMembershipAdultStudentResponseDTO dto = new GetMembershipAdultStudentResponseDTO();
            dto.setMembershipAdultStudentId(MEMBERSHIP_ADULT_STUDENT_ID);

            when(getByIdUseCase.get(MEMBERSHIP_ADULT_STUDENT_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_ADULT_STUDENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.membershipAdultStudentId").value(MEMBERSHIP_ADULT_STUDENT_ID));

            verify(getByIdUseCase).get(MEMBERSHIP_ADULT_STUDENT_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(MEMBERSHIP_ADULT_STUDENT_ID))
                    .thenThrow(new MembershipAdultStudentNotFoundException(String.valueOf(MEMBERSHIP_ADULT_STUDENT_ID)));
            when(messageService.getMembershipAdultStudentNotFound(String.valueOf(MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn("MembershipAdultStudent not found: " + MEMBERSHIP_ADULT_STUDENT_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_ADULT_STUDENT_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(MEMBERSHIP_ADULT_STUDENT_ID);
            verify(messageService).getMembershipAdultStudentNotFound(String.valueOf(MEMBERSHIP_ADULT_STUDENT_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
