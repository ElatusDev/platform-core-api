/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.interfaceadapters;

import com.akademiaplus.config.PeopleControllerAdvice;
import com.akademiaplus.customer.tutor.usecases.DeleteTutorUseCase;
import com.akademiaplus.customer.tutor.usecases.GetAllTutorsUseCase;
import com.akademiaplus.customer.tutor.usecases.GetTutorByIdUseCase;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import com.akademiaplus.customer.tutor.usecases.TutorUpdateUseCase;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.user.management.dto.GetTutorResponseDTO;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller layer component tests for TutorController.
 * Tests HTTP layer with mocked use cases using standalone MockMvc setup.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("TutorController")
@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    private static final Long TUTOR_ID = 100L;
    private static final String BASE_PATH = "/v1/user-management/tutors";

    @Mock
    private TutorCreationUseCase tutorCreationUseCase;

    @Mock
    private TutorUpdateUseCase tutorUpdateUseCase;

    @Mock
    private GetAllTutorsUseCase getAllTutorsUseCase;

    @Mock
    private GetTutorByIdUseCase getTutorByIdUseCase;

    @Mock
    private DeleteTutorUseCase deleteTutorUseCase;

    @Mock
    private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TutorController controller = new TutorController(
                tutorCreationUseCase, tutorUpdateUseCase, getAllTutorsUseCase,
                getTutorByIdUseCase, deleteTutorUseCase);
        PeopleControllerAdvice controllerAdvice = new PeopleControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /tutors")
    class GetAllTutors {

        @Test
        @DisplayName("Should return 200 with empty data when no tutors exist")
        void shouldReturn200WithEmptyData_whenNoTutorsExist() throws Exception {
            // Given
            when(getAllTutorsUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(getAllTutorsUseCase, times(1)).getAll();
            verifyNoMoreInteractions(getAllTutorsUseCase);
            verifyNoInteractions(tutorCreationUseCase, tutorUpdateUseCase, getTutorByIdUseCase, deleteTutorUseCase, messageService);
        }

        @Test
        @DisplayName("Should return 200 with tutor list when tutors exist")
        void shouldReturn200WithTutorList_whenTutorsExist() throws Exception {
            // Given
            GetTutorResponseDTO dto1 = new GetTutorResponseDTO();
            GetTutorResponseDTO dto2 = new GetTutorResponseDTO();
            when(getAllTutorsUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            verify(getAllTutorsUseCase, times(1)).getAll();
            verifyNoMoreInteractions(getAllTutorsUseCase);
            verifyNoInteractions(tutorCreationUseCase, tutorUpdateUseCase, getTutorByIdUseCase, deleteTutorUseCase, messageService);
        }
    }

    @Nested
    @DisplayName("GET /tutors/{tutorId}")
    class GetTutorById {

        @Test
        @DisplayName("Should return 200 with tutor when found")
        void shouldReturn200WithTutor_whenFound() throws Exception {
            // Given
            GetTutorResponseDTO dto = new GetTutorResponseDTO();
            when(getTutorByIdUseCase.get(TUTOR_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{tutorId}", TUTOR_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getTutorByIdUseCase, times(1)).get(TUTOR_ID);
            verifyNoMoreInteractions(getTutorByIdUseCase);
            verifyNoInteractions(tutorCreationUseCase, tutorUpdateUseCase, getAllTutorsUseCase, deleteTutorUseCase, messageService);
        }

        @Test
        @DisplayName("Should return 404 when tutor not found")
        void shouldReturn404_whenTutorNotFound() throws Exception {
            // Given
            when(messageService.getEntityNotFound(EntityType.TUTOR, String.valueOf(TUTOR_ID)))
                    .thenReturn("Tutor not found");
            when(getTutorByIdUseCase.get(TUTOR_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.TUTOR, String.valueOf(TUTOR_ID)));

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{tutorId}", TUTOR_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getTutorByIdUseCase, times(1)).get(TUTOR_ID);
            verify(messageService, times(1)).getEntityNotFound(EntityType.TUTOR, String.valueOf(TUTOR_ID));
            verifyNoMoreInteractions(getTutorByIdUseCase, messageService);
            verifyNoInteractions(tutorCreationUseCase, tutorUpdateUseCase, getAllTutorsUseCase, deleteTutorUseCase);
        }
    }
}
