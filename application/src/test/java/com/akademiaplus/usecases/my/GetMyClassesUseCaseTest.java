/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import openapi.akademiaplus.domain.my.dto.MyClassDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("GetMyClassesUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyClassesUseCaseTest {

    private static final Long COLLABORATOR_PROFILE_ID = 200L;
    private static final Long CLASS_ID = 10L;
    private static final Long COURSE_ID = 5L;
    private static final String COURSE_NAME = "Piano 101";
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 3, 15);
    private static final String EVENT_TITLE = "Weekly Piano Class";
    private static final String EVENT_DESCRIPTION = "Introduction to piano";
    private static final String SCHEDULE_DAY = "MONDAY";
    private static final LocalTime START_TIME = LocalTime.of(10, 0);
    private static final LocalTime END_TIME = LocalTime.of(11, 0);

    @Mock private UserContextHolder userContextHolder;
    @Mock private CourseEventRepository courseEventRepository;

    private GetMyClassesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyClassesUseCase(userContextHolder, courseEventRepository);
    }

    private CourseEventDataModel buildEvent() {
        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setCourseName(COURSE_NAME);

        ScheduleDataModel schedule = new ScheduleDataModel();
        schedule.setScheduleDay(SCHEDULE_DAY);
        schedule.setStartTime(START_TIME);
        schedule.setEndTime(END_TIME);

        CourseEventDataModel event = new CourseEventDataModel();
        event.setCourseEventId(CLASS_ID);
        event.setEventDate(EVENT_DATE);
        event.setEventTitle(EVENT_TITLE);
        event.setEventDescription(EVENT_DESCRIPTION);
        event.setCourseId(COURSE_ID);
        event.setCourse(course);
        event.setSchedule(schedule);
        return event;
    }

    @Nested
    @DisplayName("Class Retrieval")
    class ClassRetrieval {

        @Test
        @DisplayName("Should return classes when collaborator has assigned events")
        void shouldReturnClasses_whenCollaboratorHasEvents() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(courseEventRepository.findByCollaboratorId(COLLABORATOR_PROFILE_ID))
                    .thenReturn(List.of(buildEvent()));

            // When
            List<MyClassDTO> result = useCase.execute();

            // Then — state
            assertThat(result).hasSize(1);
            MyClassDTO dto = result.get(0);
            assertThat(dto.getClassId()).isEqualTo(CLASS_ID);
            assertThat(dto.getEventDate()).isEqualTo(EVENT_DATE);
            assertThat(dto.getEventTitle()).isEqualTo(EVENT_TITLE);
            assertThat(dto.getEventDescription()).isEqualTo(EVENT_DESCRIPTION);
            assertThat(dto.getCourseId()).isEqualTo(COURSE_ID);
            assertThat(dto.getCourseName()).isEqualTo(COURSE_NAME);
            assertThat(dto.getScheduleDay()).isEqualTo(SCHEDULE_DAY);
            assertThat(dto.getStartTime()).isEqualTo(START_TIME.toString());
            assertThat(dto.getEndTime()).isEqualTo(END_TIME.toString());

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, courseEventRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(courseEventRepository, times(1)).findByCollaboratorId(COLLABORATOR_PROFILE_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should return empty list when collaborator has no events")
        void shouldReturnEmptyList_whenNoEventsExist() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_COLLABORATOR);
            when(userContextHolder.requireProfileId()).thenReturn(COLLABORATOR_PROFILE_ID);
            when(courseEventRepository.findByCollaboratorId(COLLABORATOR_PROFILE_ID))
                    .thenReturn(List.of());

            // When
            List<MyClassDTO> result = useCase.execute();

            // Then — state
            assertThat(result).isEmpty();

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileType();
            verify(userContextHolder, times(1)).requireProfileId();
            verify(courseEventRepository, times(1)).findByCollaboratorId(COLLABORATOR_PROFILE_ID);
            verifyNoMoreInteractions(userContextHolder, courseEventRepository);
        }
    }

    @Nested
    @DisplayName("Profile Type Validation")
    class ProfileTypeValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when profile type is not COLLABORATOR")
        void shouldThrowIllegalStateException_whenNotCollaborator() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);

            // When / Then
            assertThatThrownBy(() -> useCase.execute())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(GetMyClassesUseCase.ERROR_NOT_COLLABORATOR);

            // Then — interactions
            verify(userContextHolder, times(1)).requireProfileType();
            verifyNoMoreInteractions(userContextHolder, courseEventRepository);
        }
    }
}
