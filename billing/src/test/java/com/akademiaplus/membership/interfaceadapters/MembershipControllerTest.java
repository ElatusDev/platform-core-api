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
import com.akademiaplus.membership.usecases.GetAllMembershipsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipCreationUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationResponseDTO;
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

@DisplayName("MembershipController")
@ExtendWith(MockitoExtension.class)
class MembershipControllerTest {

    private static final Long MEMBERSHIP_ID = 100L;
    private static final String BASE_PATH = "/v1/billing/memberships";

    @Mock private MembershipCreationUseCase creationUseCase;
    @Mock private GetAllMembershipsUseCase getAllUseCase;
    @Mock private GetMembershipByIdUseCase getByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MembershipController controller = new MembershipController(creationUseCase, getAllUseCase, getByIdUseCase);
        BillingControllerAdvice controllerAdvice = new BillingControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("POST /v1/billing/memberships")
    class CreateMembership {
        @Test
        @DisplayName("Should return 201 when membership created successfully")
        void shouldReturn201_whenMembershipCreatedSuccessfully() throws Exception {
            // Given
            MembershipCreationRequestDTO request = new MembershipCreationRequestDTO();
            request.setMembershipType("MONTHLY");
            request.setFee(99.99);
            request.setDescription("Monthly membership");

            MembershipCreationResponseDTO response = new MembershipCreationResponseDTO();
            response.setMembershipId(MEMBERSHIP_ID);

            when(creationUseCase.create(request)).thenReturn(response);

            // When & Then
            mockMvc.perform(post(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.membershipId").value(MEMBERSHIP_ID));

            verify(creationUseCase).create(request);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/memberships")
    class GetAllMemberships {
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
            GetMembershipResponseDTO dto1 = new GetMembershipResponseDTO();
            dto1.setMembershipId(100L);

            GetMembershipResponseDTO dto2 = new GetMembershipResponseDTO();
            dto2.setMembershipId(101L);

            when(getAllUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].membershipId").value(100L))
                    .andExpect(jsonPath("$[1].membershipId").value(101L));

            verify(getAllUseCase).getAll();
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }
    }

    @Nested
    @DisplayName("GET /v1/billing/memberships/{id}")
    class GetMembershipById {
        @Test
        @DisplayName("Should return 200 when found")
        void shouldReturn200_whenFound() throws Exception {
            // Given
            GetMembershipResponseDTO dto = new GetMembershipResponseDTO();
            dto.setMembershipId(MEMBERSHIP_ID);

            when(getByIdUseCase.get(MEMBERSHIP_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.membershipId").value(MEMBERSHIP_ID));

            verify(getByIdUseCase).get(MEMBERSHIP_ID);
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when not found")
        void shouldReturn404_whenNotFound() throws Exception {
            // Given
            when(getByIdUseCase.get(MEMBERSHIP_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.MEMBERSHIP, String.valueOf(MEMBERSHIP_ID)));
            when(messageService.getEntityNotFound(EntityType.MEMBERSHIP, String.valueOf(MEMBERSHIP_ID)))
                    .thenReturn("Membership not found: " + MEMBERSHIP_ID);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/" + MEMBERSHIP_ID))
                    .andExpect(status().isNotFound());

            verify(getByIdUseCase).get(MEMBERSHIP_ID);
            verify(messageService).getEntityNotFound(EntityType.MEMBERSHIP, String.valueOf(MEMBERSHIP_ID));
            verifyNoMoreInteractions(creationUseCase, getAllUseCase, getByIdUseCase, messageService);
        }
    }
}
