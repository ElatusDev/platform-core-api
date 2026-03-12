/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("GetAllCourseEventsUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllCourseEventsUseCaseTest {

    @Mock private CourseEventRepository courseEventRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllCourseEventsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllCourseEventsUseCase(courseEventRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no course events exist")
        void shouldReturnEmptyList_whenNoCourseEventsExist() {
            // Given
            when(courseEventRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetCourseEventResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).isEmpty();
            verify(courseEventRepository, times(1)).findAll();
            verifyNoMoreInteractions(courseEventRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when course events exist")
        void shouldReturnMappedDtos_whenCourseEventsExist() {
            // Given
            CourseEventDataModel courseEvent1 = new CourseEventDataModel();
            CourseEventDataModel courseEvent2 = new CourseEventDataModel();
            GetCourseEventResponseDTO dto1 = new GetCourseEventResponseDTO();
            GetCourseEventResponseDTO dto2 = new GetCourseEventResponseDTO();

            when(courseEventRepository.findAll()).thenReturn(List.of(courseEvent1, courseEvent2));
            when(modelMapper.map(courseEvent1, GetCourseEventResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(courseEvent2, GetCourseEventResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetCourseEventResponseDTO> result = useCase.getAll(null);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            InOrder inOrder = inOrder(courseEventRepository, modelMapper);
            inOrder.verify(courseEventRepository, times(1)).findAll();
            inOrder.verify(modelMapper, times(1)).map(courseEvent1, GetCourseEventResponseDTO.class);
            inOrder.verify(modelMapper, times(1)).map(courseEvent2, GetCourseEventResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Filtered Retrieval")
    class FilteredRetrieval {

        @Test
        @DisplayName("Should return filtered results when attendeeId is provided")
        void shouldReturnFilteredResults_whenAttendeeIdProvided() {
            // Given
            Long attendeeId = 42L;
            CourseEventDataModel courseEvent = new CourseEventDataModel();
            GetCourseEventResponseDTO dto = new GetCourseEventResponseDTO();

            when(courseEventRepository.findByAttendeeId(attendeeId)).thenReturn(List.of(courseEvent));
            when(modelMapper.map(courseEvent, GetCourseEventResponseDTO.class)).thenReturn(dto);

            // When
            List<GetCourseEventResponseDTO> result = useCase.getAll(attendeeId);

            // Then
            assertThat(result).containsExactly(dto);
            InOrder inOrder = inOrder(courseEventRepository, modelMapper);
            inOrder.verify(courseEventRepository, times(1)).findByAttendeeId(attendeeId);
            inOrder.verify(modelMapper, times(1)).map(courseEvent, GetCourseEventResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
