/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.CourseEventUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CourseEventUpdateUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CourseEventUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class CourseEventUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COURSE_EVENT_ID = 300L;
    private static final Long COURSE_ID = 42L;
    private static final Long INSTRUCTOR_ID = 10L;
    private static final Long SCHEDULE_ID = 20L;
    private static final Integer ADULT_ATTENDEE_ID_1 = 50;
    private static final Integer ADULT_ATTENDEE_ID_2 = 51;
    private static final Integer MINOR_ATTENDEE_ID_1 = 60;
    private static final Integer MINOR_ATTENDEE_ID_2 = 61;
    private static final String EVENT_TITLE = "Midterm Exam";
    private static final String EVENT_DESCRIPTION = "Midterm examination for Mathematics 101";

    @Mock private CourseEventRepository courseEventRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private CourseEventUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CourseEventUpdateUseCase(
                courseEventRepository, courseRepository, scheduleRepository,
                collaboratorRepository, adultStudentRepository, minorStudentRepository,
                tenantContextHolder, modelMapper);
    }

    private CourseEventUpdateRequestDTO buildDto() {
        CourseEventUpdateRequestDTO dto = new CourseEventUpdateRequestDTO();
        dto.setTitle(EVENT_TITLE);
        dto.setDescription(EVENT_DESCRIPTION);
        dto.setCourseId(COURSE_ID);
        dto.setInstructorId(INSTRUCTOR_ID);
        dto.setScheduleId(SCHEDULE_ID);
        dto.setAdultAttendeeIds(List.of(ADULT_ATTENDEE_ID_1, ADULT_ATTENDEE_ID_2));
        dto.setMinorAttendeeIds(List.of(MINOR_ATTENDEE_ID_1, MINOR_ATTENDEE_ID_2));
        return dto;
    }

    private CourseEventDataModel buildExistingEvent() {
        CourseEventDataModel event = new CourseEventDataModel();
        event.setCourseEventId(COURSE_EVENT_ID);
        return event;
    }

    private void stubTenantId() {
        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
    }

    private void stubEventFound(CourseEventDataModel existing) {
        when(courseEventRepository.findById(
                new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID)))
                .thenReturn(Optional.of(existing));
    }

    private void stubCourseFound() {
        when(courseRepository.findById(
                new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                .thenReturn(Optional.of(new CourseDataModel()));
    }

    private void stubCollaboratorFound() {
        when(collaboratorRepository.findById(
                new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID)))
                .thenReturn(Optional.of(new CollaboratorDataModel()));
    }

    private void stubScheduleFound() {
        when(scheduleRepository.findById(
                new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                .thenReturn(Optional.of(new ScheduleDataModel()));
    }

    private void stubAdultAttendeesFound() {
        List<AdultStudentDataModel.AdultStudentCompositeId> compositeIds = List.of(
                new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_1.longValue()),
                new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_2.longValue()));
        AdultStudentDataModel adult1 = new AdultStudentDataModel();
        adult1.setAdultStudentId(ADULT_ATTENDEE_ID_1.longValue());
        AdultStudentDataModel adult2 = new AdultStudentDataModel();
        adult2.setAdultStudentId(ADULT_ATTENDEE_ID_2.longValue());
        when(adultStudentRepository.findAllById(compositeIds)).thenReturn(List.of(adult1, adult2));
    }

    private void stubMinorAttendeesFound() {
        List<MinorStudentDataModel.MinorStudentCompositeId> compositeIds = List.of(
                new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_ATTENDEE_ID_1.longValue()),
                new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_ATTENDEE_ID_2.longValue()));
        MinorStudentDataModel minor1 = new MinorStudentDataModel();
        minor1.setMinorStudentId(MINOR_ATTENDEE_ID_1.longValue());
        MinorStudentDataModel minor2 = new MinorStudentDataModel();
        minor2.setMinorStudentId(MINOR_ATTENDEE_ID_2.longValue());
        when(minorStudentRepository.findAllById(compositeIds)).thenReturn(List.of(minor1, minor2));
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course event does not exist")
        void shouldThrowEntityNotFoundException_whenCourseEventDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            when(courseEventRepository.findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(COURSE_EVENT_ID));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verifyNoInteractions(courseRepository, collaboratorRepository, scheduleRepository,
                    adultStudentRepository, minorStudentRepository, modelMapper);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository);
        }
    }

    @Nested
    @DisplayName("Course Validation")
    class CourseValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when course does not exist")
        void shouldThrowEntityNotFoundException_whenCourseDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            when(courseRepository.findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(COURSE_ID));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verifyNoInteractions(collaboratorRepository, scheduleRepository,
                    adultStudentRepository, minorStudentRepository);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper, courseRepository);
        }
    }

    @Nested
    @DisplayName("Instructor Validation")
    class InstructorValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when instructor does not exist")
        void shouldThrowEntityNotFoundException_whenInstructorDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            stubCourseFound();
            when(collaboratorRepository.findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INSTRUCTOR_ID));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verifyNoInteractions(scheduleRepository, adultStudentRepository, minorStudentRepository);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper,
                    courseRepository, collaboratorRepository);
        }
    }

    @Nested
    @DisplayName("Schedule Validation")
    class ScheduleValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when schedule does not exist")
        void shouldThrowEntityNotFoundException_whenScheduleDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            stubCourseFound();
            stubCollaboratorFound();
            when(scheduleRepository.findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID)))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SCHEDULE_ID));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verify(scheduleRepository, times(1)).findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verifyNoInteractions(adultStudentRepository, minorStudentRepository);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper,
                    courseRepository, collaboratorRepository, scheduleRepository);
        }
    }

    @Nested
    @DisplayName("Attendee Validation")
    class AttendeeValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when adult attendee does not exist")
        void shouldThrowEntityNotFoundException_whenAdultAttendeeDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            stubCourseFound();
            stubCollaboratorFound();
            stubScheduleFound();

            List<AdultStudentDataModel.AdultStudentCompositeId> compositeIds = List.of(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_1.longValue()),
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_2.longValue()));
            AdultStudentDataModel adult1 = new AdultStudentDataModel();
            adult1.setAdultStudentId(ADULT_ATTENDEE_ID_1.longValue());
            when(adultStudentRepository.findAllById(compositeIds)).thenReturn(List.of(adult1));

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ADULT_ATTENDEE_ID_2));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verify(scheduleRepository, times(1)).findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verify(adultStudentRepository, times(1)).findAllById(compositeIds);
            verifyNoInteractions(minorStudentRepository);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper,
                    courseRepository, collaboratorRepository, scheduleRepository, adultStudentRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when minor attendee does not exist")
        void shouldThrowEntityNotFoundException_whenMinorAttendeeDoesNotExist() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            stubCourseFound();
            stubCollaboratorFound();
            stubScheduleFound();
            stubAdultAttendeesFound();

            List<MinorStudentDataModel.MinorStudentCompositeId> compositeIds = List.of(
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_ATTENDEE_ID_1.longValue()),
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_ATTENDEE_ID_2.longValue()));
            MinorStudentDataModel minor1 = new MinorStudentDataModel();
            minor1.setMinorStudentId(MINOR_ATTENDEE_ID_1.longValue());
            when(minorStudentRepository.findAllById(compositeIds)).thenReturn(List.of(minor1));

            // When & Then
            assertThatThrownBy(() -> useCase.update(COURSE_EVENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.valueOf(MINOR_ATTENDEE_ID_2));

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verify(scheduleRepository, times(1)).findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verify(adultStudentRepository, times(1)).findAllById(List.of(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_1.longValue()),
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_ATTENDEE_ID_2.longValue())));
            verify(minorStudentRepository, times(1)).findAllById(compositeIds);
            verifyNoMoreInteractions(tenantContextHolder, courseEventRepository, modelMapper,
                    courseRepository, collaboratorRepository, scheduleRepository,
                    adultStudentRepository, minorStudentRepository);
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update course event and return response when all validations pass")
        void shouldUpdateCourseEventAndReturnResponse_whenAllValidationsPass() {
            // Given
            stubTenantId();
            CourseEventUpdateRequestDTO dto = buildDto();
            CourseEventDataModel existing = buildExistingEvent();
            stubEventFound(existing);
            doNothing().when(modelMapper).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            stubCourseFound();
            stubCollaboratorFound();
            stubScheduleFound();
            stubAdultAttendeesFound();
            stubMinorAttendeesFound();
            when(courseEventRepository.saveAndFlush(existing)).thenReturn(existing);

            // When
            CourseEventUpdateResponseDTO response = useCase.update(COURSE_EVENT_ID, dto);

            // Then
            assertThat(response.getCourseEventId()).isEqualTo(COURSE_EVENT_ID);
            assertThat(response.getMessage()).isEqualTo(CourseEventUpdateUseCase.UPDATE_SUCCESS_MESSAGE);
            assertThat(existing.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(existing.getCollaboratorId()).isEqualTo(INSTRUCTOR_ID);
            assertThat(existing.getScheduleId()).isEqualTo(SCHEDULE_ID);

            InOrder inOrder = inOrder(tenantContextHolder, courseEventRepository, modelMapper,
                    courseRepository, collaboratorRepository, scheduleRepository,
                    adultStudentRepository, minorStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(courseEventRepository, times(1)).findById(
                    new CourseEventDataModel.CourseEventCompositeId(TENANT_ID, COURSE_EVENT_ID));
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CourseEventUpdateUseCase.MAP_NAME);
            inOrder.verify(courseRepository, times(1)).findById(
                    new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(
                    new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verify(courseEventRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
