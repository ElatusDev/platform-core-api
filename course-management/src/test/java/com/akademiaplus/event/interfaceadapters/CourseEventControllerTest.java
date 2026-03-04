/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.interfaceadapters;

import com.akademiaplus.config.CoordinationControllerAdvice;
import com.akademiaplus.event.usecases.CourseEventCreationUseCase;
import com.akademiaplus.event.usecases.CourseEventUpdateUseCase;
import com.akademiaplus.event.usecases.DeleteCourseEventUseCase;
import com.akademiaplus.event.usecases.GetAllCourseEventsUseCase;
import com.akademiaplus.event.usecases.GetCourseEventByIdUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
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
 * Component tests for CourseEventController.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CourseEventController")
@ExtendWith(MockitoExtension.class)
class CourseEventControllerTest {

    private static final Long COURSE_EVENT_ID = 300L;
    private static final String BASE_PATH = "/v1/course-management/course-events";

    @Mock private CourseEventCreationUseCase courseEventCreationUseCase;
    @Mock private CourseEventUpdateUseCase courseEventUpdateUseCase;
    @Mock private DeleteCourseEventUseCase deleteCourseEventUseCase;
    @Mock private GetAllCourseEventsUseCase getAllCourseEventsUseCase;
    @Mock private GetCourseEventByIdUseCase getCourseEventByIdUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CourseEventController controller = new CourseEventController(
                courseEventCreationUseCase, courseEventUpdateUseCase,
                deleteCourseEventUseCase, getAllCourseEventsUseCase,
                getCourseEventByIdUseCase);
        CoordinationControllerAdvice controllerAdvice = new CoordinationControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /course-events")
    class GetAllCourseEvents {

        @Test
        @DisplayName("Should return 200 with empty list when no course events exist")
        void shouldReturn200WithEmptyList_whenNoCourseEventsExist() throws Exception {
            // Given
            when(getAllCourseEventsUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllCourseEventsUseCase).getAll();
            verifyNoMoreInteractions(getAllCourseEventsUseCase);
        }

        @Test
        @DisplayName("Should return 200 with course event list when course events exist")
        void shouldReturn200WithCourseEventList_whenCourseEventsExist() throws Exception {
            // Given
            GetCourseEventResponseDTO dto1 = new GetCourseEventResponseDTO();
            GetCourseEventResponseDTO dto2 = new GetCourseEventResponseDTO();
            when(getAllCourseEventsUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllCourseEventsUseCase).getAll();
            verifyNoMoreInteractions(getAllCourseEventsUseCase);
        }
    }

    @Nested
    @DisplayName("GET /course-events/{courseEventId}")
    class GetCourseEventById {

        @Test
        @DisplayName("Should return 200 with course event when found")
        void shouldReturn200WithCourseEvent_whenFound() throws Exception {
            // Given
            GetCourseEventResponseDTO dto = new GetCourseEventResponseDTO();
            when(getCourseEventByIdUseCase.get(COURSE_EVENT_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{courseEventId}", COURSE_EVENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getCourseEventByIdUseCase).get(COURSE_EVENT_ID);
            verifyNoMoreInteractions(getCourseEventByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when course event not found")
        void shouldReturn404_whenCourseEventNotFound() throws Exception {
            // Given
            when(getCourseEventByIdUseCase.get(COURSE_EVENT_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.COURSE_EVENT, String.valueOf(COURSE_EVENT_ID)));
            when(messageService.getEntityNotFound(EntityType.COURSE_EVENT, String.valueOf(COURSE_EVENT_ID)))
                    .thenReturn("Course event not found");

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{courseEventId}", COURSE_EVENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getCourseEventByIdUseCase).get(COURSE_EVENT_ID);
            verify(messageService).getEntityNotFound(EntityType.COURSE_EVENT, String.valueOf(COURSE_EVENT_ID));
            verifyNoMoreInteractions(getCourseEventByIdUseCase, messageService);
        }
    }
}
