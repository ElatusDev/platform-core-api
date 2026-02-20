/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
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

@DisplayName("GetAllSchedulesUseCase")
@ExtendWith(MockitoExtension.class)
class GetAllSchedulesUseCaseTest {

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private ModelMapper modelMapper;

    private GetAllSchedulesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetAllSchedulesUseCase(scheduleRepository, modelMapper);
    }

    @Nested
    @DisplayName("Retrieval")
    class Retrieval {

        @Test
        @DisplayName("Should return empty list when no schedules exist")
        void shouldReturnEmptyList_whenNoSchedulesExist() {
            // Given
            when(scheduleRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<GetScheduleResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).isEmpty();
            verify(scheduleRepository).findAll();
            verifyNoMoreInteractions(scheduleRepository, modelMapper);
        }

        @Test
        @DisplayName("Should return mapped DTOs when schedules exist")
        void shouldReturnMappedDtos_whenSchedulesExist() {
            // Given
            ScheduleDataModel schedule1 = new ScheduleDataModel();
            ScheduleDataModel schedule2 = new ScheduleDataModel();
            GetScheduleResponseDTO dto1 = new GetScheduleResponseDTO();
            GetScheduleResponseDTO dto2 = new GetScheduleResponseDTO();

            when(scheduleRepository.findAll()).thenReturn(List.of(schedule1, schedule2));
            when(modelMapper.map(schedule1, GetScheduleResponseDTO.class)).thenReturn(dto1);
            when(modelMapper.map(schedule2, GetScheduleResponseDTO.class)).thenReturn(dto2);

            // When
            List<GetScheduleResponseDTO> result = useCase.getAll();

            // Then
            assertThat(result).containsExactly(dto1, dto2);
            verify(scheduleRepository).findAll();
            verify(modelMapper).map(schedule1, GetScheduleResponseDTO.class);
            verify(modelMapper).map(schedule2, GetScheduleResponseDTO.class);
            verifyNoMoreInteractions(scheduleRepository, modelMapper);
        }
    }
}
