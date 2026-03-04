/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.config.CoordinationControllerAdvice;
import com.akademiaplus.program.usecases.CourseUpdateUseCase;
import com.akademiaplus.program.usecases.CreateCourseUseCase;
import com.akademiaplus.program.usecases.DeleteCourseUseCase;
import com.akademiaplus.program.usecases.GetAllCoursesUseCase;
import com.akademiaplus.program.usecases.GetCourseByIdUseCase;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
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
 * Component tests for CourseController.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CourseController")
@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private static final Long COURSE_ID = 100L;
    private static final String BASE_PATH = "/v1/course-management/courses";

    @Mock private CreateCourseUseCase createCourseUseCase;
    @Mock private DeleteCourseUseCase deleteCourseUseCase;
    @Mock private GetAllCoursesUseCase getAllCoursesUseCase;
    @Mock private GetCourseByIdUseCase getCourseByIdUseCase;
    @Mock private CourseUpdateUseCase courseUpdateUseCase;
    @Mock private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CourseController controller = new CourseController(
                createCourseUseCase, getAllCoursesUseCase, getCourseByIdUseCase,
                deleteCourseUseCase, courseUpdateUseCase);
        CoordinationControllerAdvice controllerAdvice = new CoordinationControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /courses")
    class GetAllCourses {

        @Test
        @DisplayName("Should return 200 with empty list when no courses exist")
        void shouldReturn200WithEmptyList_whenNoCoursesExist() throws Exception {
            // Given
            when(getAllCoursesUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(getAllCoursesUseCase).getAll();
            verifyNoMoreInteractions(getAllCoursesUseCase);
        }

        @Test
        @DisplayName("Should return 200 with course list when courses exist")
        void shouldReturn200WithCourseList_whenCoursesExist() throws Exception {
            // Given
            GetCourseResponseDTO dto1 = new GetCourseResponseDTO();
            GetCourseResponseDTO dto2 = new GetCourseResponseDTO();
            when(getAllCoursesUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));

            verify(getAllCoursesUseCase).getAll();
            verifyNoMoreInteractions(getAllCoursesUseCase);
        }
    }

    @Nested
    @DisplayName("GET /courses/{courseId}")
    class GetCourseById {

        @Test
        @DisplayName("Should return 200 with course when found")
        void shouldReturn200WithCourse_whenFound() throws Exception {
            // Given
            GetCourseResponseDTO dto = new GetCourseResponseDTO();
            when(getCourseByIdUseCase.get(COURSE_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{courseId}", COURSE_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getCourseByIdUseCase).get(COURSE_ID);
            verifyNoMoreInteractions(getCourseByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when course not found")
        void shouldReturn404_whenCourseNotFound() throws Exception {
            // Given
            when(getCourseByIdUseCase.get(COURSE_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.COURSE, String.valueOf(COURSE_ID)));
            when(messageService.getEntityNotFound(EntityType.COURSE, String.valueOf(COURSE_ID)))
                    .thenReturn("Course not found");

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{courseId}", COURSE_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(getCourseByIdUseCase).get(COURSE_ID);
            verify(messageService).getEntityNotFound(EntityType.COURSE, String.valueOf(COURSE_ID));
            verifyNoMoreInteractions(getCourseByIdUseCase, messageService);
        }
    }
}
