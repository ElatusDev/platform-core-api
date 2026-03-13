/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.domain;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.program.domain.exception.ScheduleConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DomainCourse}.
 *
 * <p>Plain JUnit — no Spring context required.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DomainCourse")
class DomainCourseTest {

    private static final Long COURSE_ID = 10L;
    private static final Long OTHER_COURSE_ID = 99L;

    private final DomainCourse domainCourse = new DomainCourse();

    private CourseDataModel buildCourse() {
        CourseDataModel course = new CourseDataModel();
        course.setCourseId(COURSE_ID);
        course.setTenantId(1L);
        course.setCourseName("Test Course");
        return course;
    }

    private ScheduleDataModel buildSchedule(Long scheduleId, Long courseId) {
        ScheduleDataModel schedule = new ScheduleDataModel();
        schedule.setScheduleId(scheduleId);
        schedule.setTenantId(1L);
        schedule.setCourseId(courseId);
        return schedule;
    }

    @Nested
    @DisplayName("validateScheduleConflict")
    class ValidateScheduleConflict {

        @Test
        @DisplayName("Should pass when no schedule has a course ID")
        void shouldPass_whenNoScheduleHasCourseId() {
            // Given
            CourseDataModel course = buildCourse();
            List<ScheduleDataModel> schedules = List.of(
                    buildSchedule(1L, null),
                    buildSchedule(2L, null)
            );

            // When / Then
            assertThatCode(() -> domainCourse.get(course)
                    .validateScheduleConflict(schedules, COURSE_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should pass when schedules belong to the same course")
        void shouldPass_whenSchedulesBelongToSameCourse() {
            // Given
            CourseDataModel course = buildCourse();
            List<ScheduleDataModel> schedules = List.of(
                    buildSchedule(1L, COURSE_ID),
                    buildSchedule(2L, COURSE_ID)
            );

            // When / Then
            assertThatCode(() -> domainCourse.get(course)
                    .validateScheduleConflict(schedules, COURSE_ID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw ScheduleConflictException when schedule assigned to different course")
        void shouldThrowScheduleConflictException_whenScheduleAssignedToDifferentCourse() {
            // Given
            CourseDataModel course = buildCourse();
            List<ScheduleDataModel> schedules = List.of(
                    buildSchedule(1L, null),
                    buildSchedule(2L, OTHER_COURSE_ID)
            );

            // When / Then
            assertThatThrownBy(() -> domainCourse.get(course)
                    .validateScheduleConflict(schedules, COURSE_ID))
                    .isInstanceOf(ScheduleConflictException.class)
                    .hasMessageContaining(ScheduleConflictException.ERROR_MESSAGE)
                    .hasMessageContaining("2");
        }

        @Test
        @DisplayName("Should include all conflicting IDs when multiple conflicts exist")
        void shouldIncludeConflictingIds_whenMultipleConflicts() {
            // Given
            CourseDataModel course = buildCourse();
            List<ScheduleDataModel> schedules = List.of(
                    buildSchedule(3L, OTHER_COURSE_ID),
                    buildSchedule(5L, OTHER_COURSE_ID)
            );

            // When / Then
            assertThatThrownBy(() -> domainCourse.get(course)
                    .validateScheduleConflict(schedules, COURSE_ID))
                    .isInstanceOf(ScheduleConflictException.class)
                    .hasMessageContaining(ScheduleConflictException.ERROR_MESSAGE)
                    .hasMessageContaining("3")
                    .hasMessageContaining("5");
        }

        @Test
        @DisplayName("Should pass when schedule list is empty")
        void shouldPass_whenScheduleListIsEmpty() {
            // Given
            CourseDataModel course = buildCourse();

            // When / Then
            assertThatCode(() -> domainCourse.get(course)
                    .validateScheduleConflict(List.of(), COURSE_ID))
                    .doesNotThrowAnyException();
        }
    }
}
