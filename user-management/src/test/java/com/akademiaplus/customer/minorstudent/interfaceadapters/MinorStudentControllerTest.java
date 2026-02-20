/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.interfaceadapters;

import com.akademiaplus.config.PeopleControllerAdvice;
import com.akademiaplus.customer.minorstudent.usecases.DeleteMinorStudentUseCase;
import com.akademiaplus.customer.minorstudent.usecases.GetAllMinorStudentsUseCase;
import com.akademiaplus.customer.minorstudent.usecases.GetMinorStudentByIdUseCase;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.user.management.dto.GetMinorStudentResponseDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller layer component tests for MinorStudentController.
 * Tests HTTP layer with mocked use cases using standalone MockMvc setup.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MinorStudentController")
@ExtendWith(MockitoExtension.class)
class MinorStudentControllerTest {

    private static final Long MINOR_STUDENT_ID = 200L;
    private static final String BASE_PATH = "/v1/user-management/minor-students";

    @Mock
    private GetAllMinorStudentsUseCase getAllMinorStudentsUseCase;

    @Mock
    private GetMinorStudentByIdUseCase getMinorStudentByIdUseCase;

    @Mock
    private DeleteMinorStudentUseCase deleteMinorStudentUseCase;

    @Mock
    private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MinorStudentController controller = new MinorStudentController(
                getAllMinorStudentsUseCase, getMinorStudentByIdUseCase,
                deleteMinorStudentUseCase);
        PeopleControllerAdvice controllerAdvice = new PeopleControllerAdvice(messageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(controllerAdvice)
                .build();
    }

    @Nested
    @DisplayName("GET /minor-students")
    class GetAllMinorStudents {

        @Test
        @DisplayName("Should return 200 with empty data when no minor students exist")
        void shouldReturn200WithEmptyData_whenNoMinorStudentsExist() throws Exception {
            // Given
            when(getAllMinorStudentsUseCase.getAll()).thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data", hasSize(0)));

            verify(getAllMinorStudentsUseCase).getAll();
            verifyNoMoreInteractions(getAllMinorStudentsUseCase);
        }

        @Test
        @DisplayName("Should return 200 with minor student list when minor students exist")
        void shouldReturn200WithMinorStudentList_whenMinorStudentsExist() throws Exception {
            // Given
            GetMinorStudentResponseDTO dto1 = new GetMinorStudentResponseDTO();
            GetMinorStudentResponseDTO dto2 = new GetMinorStudentResponseDTO();
            when(getAllMinorStudentsUseCase.getAll()).thenReturn(List.of(dto1, dto2));

            // When & Then
            mockMvc.perform(get(BASE_PATH).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.data", hasSize(2)));

            verify(getAllMinorStudentsUseCase).getAll();
            verifyNoMoreInteractions(getAllMinorStudentsUseCase);
        }
    }

    @Nested
    @DisplayName("GET /minor-students/{minorStudentId}")
    class GetMinorStudentById {

        @Test
        @DisplayName("Should return 200 with minor student when found")
        void shouldReturn200WithMinorStudent_whenFound() throws Exception {
            // Given
            GetMinorStudentResponseDTO dto = new GetMinorStudentResponseDTO();
            when(getMinorStudentByIdUseCase.get(MINOR_STUDENT_ID)).thenReturn(dto);

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{minorStudentId}", MINOR_STUDENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getMinorStudentByIdUseCase).get(MINOR_STUDENT_ID);
            verifyNoMoreInteractions(getMinorStudentByIdUseCase);
        }

        @Test
        @DisplayName("Should return 404 when minor student not found")
        void shouldReturn404_whenMinorStudentNotFound() throws Exception {
            // Given
            when(messageService.getEntityNotFound(EntityType.MINOR_STUDENT, String.valueOf(MINOR_STUDENT_ID)))
                    .thenReturn("Minor student not found");
            when(getMinorStudentByIdUseCase.get(MINOR_STUDENT_ID))
                    .thenThrow(new EntityNotFoundException(EntityType.MINOR_STUDENT, String.valueOf(MINOR_STUDENT_ID)));

            // When & Then
            mockMvc.perform(get(BASE_PATH + "/{minorStudentId}", MINOR_STUDENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(getMinorStudentByIdUseCase).get(MINOR_STUDENT_ID);
            verifyNoMoreInteractions(getMinorStudentByIdUseCase);
        }
    }
}
