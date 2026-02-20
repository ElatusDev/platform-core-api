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
import com.akademiaplus.membership.usecases.DeleteMembershipTutorUseCase;
import com.akademiaplus.membership.usecases.GetAllMembershipTutorsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipTutorByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipTutorCreationUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationResponseDTO;
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

@DisplayName("MembershipTutorController")
@ExtendWith(MockitoExtension.class)
class MembershipTutorControllerTest {

    private static final Long MEMBERSHIP_TUTOR_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/membershipTutors";

    @Mock private MembershipTutorCreationUseCase creationUseCase;
    @Mock private GetAllMembershipTutorsUseCase getAllUseCase;
    @Mock private GetMembershipTutorByIdUseCase getByIdUseCase;
    @Mock private DeleteMembershipTutorUseCase deleteMembershipTutorUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MembershipTutorController controller = new MembershipTutorController(
                creationUseCase, getAllUseCase, getByIdUseCase, deleteMembershipTutorUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/membershipTutors")
    class CreateMembershipTutor {
        @Test
        @DisplayName("Should return 201 when membership-tutor created successfully")
        void shouldReturn201_whenMembershipTutorCreatedSuccessfully() throws Exception {
            // Given
            MembershipTutorCreationRequestDTO request = new MembershipTutorCreationRequestDTO();
            request.setStartDate(LocalDate.of(2026, 1, 1));
            request.setDueDate(LocalDate.of(2026, 2, 1));
            request.setMembershipId(1L);
            request.setCourseId(2L);
            request.setTutorId(3L);

            MembershipTutorCreationResponseDTO response = new MembershipTutorCreationResponseDTO();
            response.setMembershipTutorId(MEMBERSHIP_TUTOR_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.membershipTutorId").value(MEMBERSHIP_TUTOR_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/membershipTutors")
    class GetAllMembershipTutors {
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
            GetMembershipTutorResponseDTO dto1 = new GetMembershipTutorResponseDTO();
            dto1.setMembershipTutorId(100L);

            GetMembershipTutorResponseDTO dto2 = new GetMembershipTutorResponseDTO();
            dto2.setMembershipTutorId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].membershipTutorId").value(100L))
                    .andExpect(jsonPath("$[1].membershipTutorId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/membershipTutors/{id}")
    class GetMembershipTutorById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetMembershipTutorResponseDTO dto = new GetMembershipTutorResponseDTO();
            dto.setMembershipTutorId(MEMBERSHIP_TUTOR_ID);

            when(getByIdUseCase.get(MEMBERSHIP_TUTOR_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_TUTOR_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.membershipTutorId").value(MEMBERSHIP_TUTOR_ID));

            verify(getByIdUseCase).get(MEMBERSHIP_TUTOR_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(MEMBERSHIP_TUTOR_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.MEMBERSHIP_TUTOR, String.valueOf(MEMBERSHIP_TUTOR_ID)));
            when(messageService.getEntityNotFound(EntityType.MEMBERSHIP_TUTOR, String.valueOf(MEMBERSHIP_TUTOR_ID)))
                    .thenReturn("MembershipTutor not found: " + MEMBERSHIP_TUTOR_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_TUTOR_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(MEMBERSHIP_TUTOR_ID);
            verify(messageService).getEntityNotFound(EntityType.MEMBERSHIP_TUTOR, String.valueOf(MEMBERSHIP_TUTOR_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
