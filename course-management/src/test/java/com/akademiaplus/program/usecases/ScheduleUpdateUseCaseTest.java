/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.ScheduleUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScheduleUpdateUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("ScheduleUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class ScheduleUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long SCHEDULE_ID = 42L;
    private static final Long COURSE_ID = 5L;
    private static final String SCHEDULE_DAY = "Monday";
    private static final String START_TIME = "09:00";
    private static final String END_TIME = "10:30";

    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private ScheduleUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ScheduleUpdateUseCase(
                scheduleRepository, courseRepository,
                tenantContextHolder, modelMapper);
    }

    private ScheduleUpdateRequestDTO buildDto() {
        ScheduleUpdateRequestDTO dto = new ScheduleUpdateRequestDTO();
        dto.setScheduleDay(SCHEDULE_DAY);
        dto.setStartTime(START_TIME);
        dto.setEndTime(END_TIME);
        dto.setCourseId(COURSE_ID);
        return dto;
    }

    private ScheduleDataModel buildExistingSchedule() {
        ScheduleDataModel schedule = new ScheduleDataModel();
        schedule.setScheduleId(SCHEDULE_ID);
        schedule.setCourseId(COURSE_ID);
        return schedule;
    }

    private void stubTenantId() {
        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
    }

    private void stubScheduleFound(ScheduleDataModel existing) {
        when(scheduleRepository.findById(
                new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                .thenReturn(Optional.of(existing));
    }

    private void stubCourseFound() {
        when(courseRepository.findById(
                new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                .thenReturn(Optional.of(new CourseDataModel()));
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when schedule does not exist")
        void shouldThrowEntityNotFoundException_whenScheduleDoesNotExist() {
            // Given
            stubTenantId();
            ScheduleUpdateRequestDTO dto = buildDto();
            when(scheduleRepository.findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(SCHEDULE_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SCHEDULE_ID));
        }
    }

    @Nested
    @DisplayName("Course Validation")
    class CourseValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when courseId references nonexistent course")
        void shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
            // Given
            stubTenantId();
            ScheduleUpdateRequestDTO dto = buildDto();
            ScheduleDataModel existing = buildExistingSchedule();
            stubScheduleFound(existing);
            doNothing().when(modelMapper).map(dto, existing, ScheduleUpdateUseCase.MAP_NAME);
            when(courseRepository.findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(SCHEDULE_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(COURSE_ID));
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update schedule and return response when schedule exists")
        void shouldUpdateScheduleAndReturnResponse_whenScheduleExists() {
            // Given
            stubTenantId();
            ScheduleUpdateRequestDTO dto = buildDto();
            ScheduleDataModel existing = buildExistingSchedule();
            stubScheduleFound(existing);
            doNothing().when(modelMapper).map(dto, existing, ScheduleUpdateUseCase.MAP_NAME);
            stubCourseFound();
            when(scheduleRepository.saveAndFlush(existing)).thenReturn(existing);

            // When
            ScheduleUpdateResponseDTO response = useCase.update(SCHEDULE_ID, dto);

            // Then
            verify(scheduleRepository).saveAndFlush(existing);
            assertThat(response.getScheduleId()).isEqualTo(SCHEDULE_ID);
            assertThat(response.getMessage()).isEqualTo(ScheduleUpdateUseCase.UPDATE_SUCCESS_MESSAGE);
        }
    }
}
