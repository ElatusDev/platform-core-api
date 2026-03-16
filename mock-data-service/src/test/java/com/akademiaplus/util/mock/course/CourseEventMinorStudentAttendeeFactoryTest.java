/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CourseEventMinorStudentAttendeeFactory}.
 */
@DisplayName("CourseEventMinorStudentAttendeeFactory")
class CourseEventMinorStudentAttendeeFactoryTest {

    private CourseEventMinorStudentAttendeeFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CourseEventMinorStudentAttendeeFactory();
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and all IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndAllIdsSet() {
            // Given
            factory.setAvailableCourseEventIds(List.of(10L, 20L));
            factory.setAvailableMinorStudentIds(List.of(30L, 40L));

            // When
            List<CourseEventMinorStudentAttendeeRecord> result = factory.generate(3);

            // Then
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableCourseEventIds(List.of(10L));
            factory.setAvailableMinorStudentIds(List.of(30L));

            // When
            List<CourseEventMinorStudentAttendeeRecord> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when course event IDs are not set")
        void shouldThrowIllegalStateException_whenCourseEventIdsNotSet() {
            // Given
            factory.setAvailableMinorStudentIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(CourseEventMinorStudentAttendeeFactory.ERROR_COURSE_EVENT_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when minor student IDs are not set")
        void shouldThrowIllegalStateException_whenMinorStudentIdsNotSet() {
            // Given
            factory.setAvailableCourseEventIds(List.of(10L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(CourseEventMinorStudentAttendeeFactory.ERROR_MINOR_STUDENT_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Deduplication")
    class Deduplication {

        @Test
        @DisplayName("Should produce unique pairs only")
        void shouldProduceUniquePairsOnly() {
            // Given
            factory.setAvailableCourseEventIds(List.of(1L, 2L));
            factory.setAvailableMinorStudentIds(List.of(100L, 200L));

            // When
            List<CourseEventMinorStudentAttendeeRecord> result = factory.generate(4);

            // Then
            long uniqueKeys = result.stream()
                    .map(r -> r.courseEventId() + ":" + r.minorStudentId())
                    .distinct().count();
            assertThat(uniqueKeys).isEqualTo(result.size());
        }
    }

    @Nested
    @DisplayName("Record population")
    class RecordPopulation {

        @Test
        @DisplayName("Should populate all fields when generating single record")
        void shouldPopulateAllFields_whenGeneratingSingleRecord() {
            // Given
            factory.setAvailableCourseEventIds(List.of(10L));
            factory.setAvailableMinorStudentIds(List.of(30L));

            // When
            List<CourseEventMinorStudentAttendeeRecord> result = factory.generate(1);
            CourseEventMinorStudentAttendeeRecord record = result.get(0);

            // Then
            assertThat(record.courseEventId()).isEqualTo(10L);
            assertThat(record.minorStudentId()).isEqualTo(30L);
        }
    }
}
