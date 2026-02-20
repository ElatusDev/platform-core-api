/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.config.CoordinationControllerAdvice;
import com.akademiaplus.program.usecases.GetAllSchedulesUseCase;
import com.akademiaplus.program.usecases.GetScheduleByIdUseCase;
import com.akademiaplus.program.usecases.ScheduleCreationUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
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

/**
 * Component tests for ScheduleController.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("ScheduleController")
@ExtendWith(MockitoExtension.class)
class ScheduleControllerTest {

    private static final Long SCHEDULE_ID = 200L;
    private static final String BASE_PATH = "/v1/course-management/schedules";

    @Mock private ScheduleCreationUseCase scheduleCreationUseCase;
    @Mock private GetAllSchedulesUseCase getAllSchedulesUseCase;
    @Mock private GetScheduleByIdUseCase getScheduleByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScheduleController controller = new ScheduleController(
                scheduleCreationUseCase, getAllSchedulesUseCase, getScheduleByIdUseCase);
        CoordinationControllerAdvice controllerAdvice = new CoordinationControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /schedules")
    class GetAllSchedules {

        @Test
        @DisplayName("Should return 200 with empty list when no schedules exist")
        void shouldReturn200WithEmptyList_whenNoSchedulesExist() throws Exception {
            // Given
            when(getAllSchedulesUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllSchedulesUseCase).getAll();
            verifyNoMoreInteractions(getAllSchedulesUseCase);
        }

        @Test
        @DisplayName("Should return 200 with schedule list when schedules exist")
        void shouldReturn200WithScheduleList_whenSchedulesExist() throws Exception {
            // Given
            GetScheduleResponseDTO dto1 = new GetScheduleResponseDTO();
            GetScheduleResponseDTO dto2 = new GetScheduleResponseDTO();
            when(getAllSchedulesUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllSchedulesUseCase).getAll();
            verifyNoMoreInteractions(getAllSchedulesUseCase);
        }
    }

    @Nested
    @DisplayName("GET /schedules/{scheduleId}")
    class GetScheduleById {

        @Test
        @DisplayName("Should return 200 with schedule when found")
        void shouldReturn200WithSchedule_whenFound() throws Exception {
            // Given
            GetScheduleResponseDTO dto = new GetScheduleResponseDTO();
            when(getScheduleByIdUseCase.get(SCHEDULE_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{scheduleId}", SCHEDULE_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getScheduleByIdUseCase).get(SCHEDULE_ID);
            verifyNoMoreInteractions(getScheduleByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when schedule not found")
        void shouldReturn404_whenScheduleNotFound() throws Exception {
            // Given
            when(getScheduleByIdUseCase.get(SCHEDULE_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.SCHEDULE, String.valueOf(SCHEDULE_ID)));
            when(messageService.getEntityNotFound(EntityType.SCHEDULE, String.valueOf(SCHEDULE_ID)))
                    .thenReturn("Schedule not found");

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{scheduleId}", SCHEDULE_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getScheduleByIdUseCase).get(SCHEDULE_ID);
            verify(messageService).getEntityNotFound(EntityType.SCHEDULE, String.valueOf(SCHEDULE_ID));
            verifyNoMoreInteractions(getScheduleByIdUseCase, messageService);
        }
    }
}
