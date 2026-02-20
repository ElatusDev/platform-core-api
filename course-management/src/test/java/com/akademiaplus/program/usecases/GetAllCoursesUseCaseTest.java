/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllCoursesUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllCoursesUseCaseTest {

    @Mock private CourseRepository courseRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllCoursesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllCoursesUseCase(courseRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no courses exist")
        void shouldReturnEmptyList_whenNoCoursesExist() {
            // Given
            when(courseRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetCourseResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(courseRepository).findAll();
            verifyNoMoreInteractions(courseRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when courses exist")
        void shouldReturnMappedDtos_whenCoursesExist() {
            // Given
            CourseDataModel course1 = new CourseDataModel();
            CourseDataModel course2 = new CourseDataModel();
            GetCourseResponseDTO dto1 = new GetCourseResponseDTO();
            GetCourseResponseDTO dto2 = new GetCourseResponseDTO();

            when(courseRepository.findAll()).thenReturn(List.of(course1, course2));
            when(modelMapper.map(course1, GetCourseResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(course2, GetCourseResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetCourseResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(courseRepository).findAll();
            verify(modelMapper).map(course1, GetCourseResponseDTO.class);
            verify(modelMapper).map(course2, GetCourseResponseDTO.class);
            verifyNoMoreInteractions(courseRepository, modelMapper);
        }
    }
}
