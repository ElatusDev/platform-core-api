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
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.CourseUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CourseUpdateUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CourseUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class CourseUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_ID = 42L;
    private static final Long COLLABORATOR_ID_1 = 10L;
    private static final Long COLLABORATOR_ID_2 = 11L;
    private static final Long SCHEDULE_ID_1 = 20L;
    private static final Long SCHEDULE_ID_2 = 21L;
    private static final String COURSE_NAME = "Mathematics 101";
    private static final String COURSE_DESCRIPTION = "Introduction to mathematics";
    private static final Integer MAX_CAPACITY = 30;

    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CourseValidator courseValidator;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private CourseUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CourseUpdateUseCase(
                courseRepository, scheduleRepository, courseValidator,
                tenantContextHolder, modelMapper);
    }

    private CourseUpdateRequestDTO buildDto() {
        CourseUpdateRequestDTO dto = new CourseUpdateRequestDTO();
        dto.setName(COURSE_NAME);
        dto.setDescription(COURSE_DESCRIPTION);
        dto.setMaxCapacity(MAX_CAPACITY);
        dto.setAvailableCollaboratorIds(List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2));
        dto.setTimeTableIds(List.of(SCHEDULE_ID_1));
        return dto;
    }

    private CourseDataModel buildExistingCourse() {
        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setSchedules(new ArrayList<>());
        course.setAvailableCollaborators(new ArrayList<>());
        return course;
    }

    private ScheduleDataModel buildSchedule(Long scheduleId, Long assignedCourseId) {
        ScheduleDataModel schedule = new ScheduleDataModel();
        schedule.setScheduleId(scheduleId);
        schedule.setCourseId(assignedCourseId);
        return schedule;
    }

    private void stubTenantId() {
        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
    }

    private void stubCourseFound(CourseDataModel existing) {
        when(courseRepository.findById(
                new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                .thenReturn(Optional.of(existing));
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course does not exist")
        void shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            when(courseRepository.findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(COURSE_ID));
        }
    }

    @Nested
    @DisplayName("Collaborator Validation")
    class CollaboratorValidation {

        @Test
        @DisplayName("Should delegate collaborator validation to CourseValidator")
        void shouldDelegateCollaboratorValidation_toCourseValidator() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            CourseDataModel existing = buildExistingCourse();
            stubCourseFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseUpdateUseCase.MAP_NAME);

            List<Long> collaboratorIds = List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2);
            CollaboratorDataModel collab1 = new CollaboratorDataModel();
            CollaboratorDataModel collab2 = new CollaboratorDataModel();
            when(courseValidator.validateCollaboratorsExist(collaboratorIds))
                    .thenReturn(List.of(collab1, collab2));

            ScheduleDataModel schedule1 = buildSchedule(SCHEDULE_ID_1, null);
            List<ScheduleDataModel.ScheduleCompositeId> compositeIds =
                    List.of(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID_1));
            when(scheduleRepository.findAllById(compositeIds)).thenReturn(List.of(schedule1));
            when(courseRepository.saveAndFlush(existing)).thenReturn(existing);

            // When
            useCase.update(COURSE_ID, dto);

            // Then
            verify(courseValidator).validateCollaboratorsExist(collaboratorIds);
            assertThat(existing.getAvailableCollaborators()).containsExactly(collab1, collab2);
        }
    }

    @Nested
    @DisplayName("Schedule Reassignment")
    class ScheduleReassignment {

        @Test
        @DisplayName("Should unlink old schedules not in new list")
        void shouldUnlinkOldSchedules_whenNotInNewList() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            dto.setTimeTableIds(List.of(SCHEDULE_ID_2));

            ScheduleDataModel oldSchedule = buildSchedule(SCHEDULE_ID_1, COURSE_ID);
            CourseDataModel existing = buildExistingCourse();
            existing.setSchedules(new ArrayList<>(List.of(oldSchedule)));
            stubCourseFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseUpdateUseCase.MAP_NAME);

            List<Long> collaboratorIds = List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2);
            when(courseValidator.validateCollaboratorsExist(collaboratorIds))
                    .thenReturn(List.of(new CollaboratorDataModel()));

            ScheduleDataModel newSchedule = buildSchedule(SCHEDULE_ID_2, null);
            List<ScheduleDataModel.ScheduleCompositeId> compositeIds =
                    List.of(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID_2));
            when(scheduleRepository.findAllById(compositeIds)).thenReturn(List.of(newSchedule));
            when(courseRepository.saveAndFlush(existing)).thenReturn(existing);

            // When
            useCase.update(COURSE_ID, dto);

            // Then
            assertThat(oldSchedule.getCourseId()).isNull();
            verify(scheduleRepository).saveAll(List.of(oldSchedule));
        }

        @Test
        @DisplayName("Should throw ScheduleNotAvailableException when schedule assigned to another course")
        void shouldThrowScheduleNotAvailableException_whenScheduleAssignedToAnotherCourse() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            CourseDataModel existing = buildExistingCourse();
            stubCourseFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseUpdateUseCase.MAP_NAME);

            List<Long> collaboratorIds = List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2);
            when(courseValidator.validateCollaboratorsExist(collaboratorIds))
                    .thenReturn(List.of(new CollaboratorDataModel()));

            Long otherCourseId = 999L;
            ScheduleDataModel conflictingSchedule = buildSchedule(SCHEDULE_ID_1, otherCourseId);
            List<ScheduleDataModel.ScheduleCompositeId> compositeIds =
                    List.of(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID_1));
            when(scheduleRepository.findAllById(compositeIds)).thenReturn(List.of(conflictingSchedule));

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_ID, dto))
                    .isInstanceOf(ScheduleNotAvailableException.class);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when schedule does not exist")
        void shouldThrowEntityNotFoundException_whenScheduleDoesNotExist() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            CourseDataModel existing = buildExistingCourse();
            stubCourseFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseUpdateUseCase.MAP_NAME);

            List<Long> collaboratorIds = List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2);
            when(courseValidator.validateCollaboratorsExist(collaboratorIds))
                    .thenReturn(List.of(new CollaboratorDataModel()));

            List<ScheduleDataModel.ScheduleCompositeId> compositeIds =
                    List.of(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID_1));
            when(scheduleRepository.findAllById(compositeIds)).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SCHEDULE_ID_1));
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update course and return response when course exists")
        void shouldUpdateCourseAndReturnResponse_whenCourseExists() {
            // Given
            stubTenantId();
            CourseUpdateRequestDTO dto = buildDto();
            CourseDataModel existing = buildExistingCourse();
            stubCourseFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseUpdateUseCase.MAP_NAME);

            List<Long> collaboratorIds = List.of(COLLABORATOR_ID_1, COLLABORATOR_ID_2);
            CollaboratorDataModel collab1 = new CollaboratorDataModel();
            when(courseValidator.validateCollaboratorsExist(collaboratorIds))
                    .thenReturn(List.of(collab1));

            ScheduleDataModel schedule1 = buildSchedule(SCHEDULE_ID_1, null);
            List<ScheduleDataModel.ScheduleCompositeId> compositeIds =
                    List.of(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID_1));
            when(scheduleRepository.findAllById(compositeIds)).thenReturn(List.of(schedule1));
            when(courseRepository.saveAndFlush(existing)).thenReturn(existing);

            // When
            CourseUpdateResponseDTO response = useCase.update(COURSE_ID, dto);

            // Then
            verify(courseRepository).saveAndFlush(existing);
            assertThat(response.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(response.getMessage()).isEqualTo(CourseUpdateUseCase.UPDATE_SUCCESS_MESSAGE);
            assertThat(schedule1.getCourseId()).isEqualTo(COURSE_ID);
        }
    }
}
