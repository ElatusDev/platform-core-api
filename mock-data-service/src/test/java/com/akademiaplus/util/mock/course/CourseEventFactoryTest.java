/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CourseEventFactory}.
 */
@DisplayName("CourseEventFactory")
class CourseEventFactoryTest {

    private CourseEventFactory factory;

    @BeforeEach
    void setUp() {
        CourseEventDataGenerator generator = new CourseEventDataGenerator();
        factory = new CourseEventFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and all IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndAllIdsSet() {
            // Given
            int expectedCount = 3;
            factory.setAvailableCourseIds(List.of(10L));
            factory.setAvailableScheduleIds(List.of(20L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When
            List<CourseEventCreateRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;
            factory.setAvailableCourseIds(List.of(10L));
            factory.setAvailableScheduleIds(List.of(20L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When
            List<CourseEventCreateRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when course IDs are not set")
        void shouldThrowIllegalStateException_whenCourseIdsNotSet() {
            // Given
            factory.setAvailableScheduleIds(List.of(20L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("availableCourseIds must be set");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when schedule IDs are not set")
        void shouldThrowIllegalStateException_whenScheduleIdsNotSet() {
            // Given
            factory.setAvailableCourseIds(List.of(10L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("availableScheduleIds must be set");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when collaborator IDs are not set")
        void shouldThrowIllegalStateException_whenCollaboratorIdsNotSet() {
            // Given
            factory.setAvailableCourseIds(List.of(10L));
            factory.setAvailableScheduleIds(List.of(20L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("availableCollaboratorIds must be set");
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign IDs in round-robin fashion across all FK lists")
        void shouldAssignIdsInRoundRobinFashion() {
            // Given
            factory.setAvailableCourseIds(List.of(100L, 200L));
            factory.setAvailableScheduleIds(List.of(300L, 400L));
            factory.setAvailableCollaboratorIds(List.of(500L, 600L));

            // When
            List<CourseEventCreateRequestDTO> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getCourseId()).isEqualTo(100L);
            assertThat(result.get(1).getCourseId()).isEqualTo(200L);
            assertThat(result.get(2).getCourseId()).isEqualTo(100L);
            assertThat(result.get(3).getCourseId()).isEqualTo(200L);

            assertThat(result.get(0).getScheduleId()).isEqualTo(300L);
            assertThat(result.get(1).getScheduleId()).isEqualTo(400L);

            assertThat(result.get(0).getInstructorId()).isEqualTo(500L);
            assertThat(result.get(1).getInstructorId()).isEqualTo(600L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single event")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleEvent() {
            // Given
            factory.setAvailableCourseIds(List.of(10L));
            factory.setAvailableScheduleIds(List.of(20L));
            factory.setAvailableCollaboratorIds(List.of(30L));

            // When
            List<CourseEventCreateRequestDTO> result = factory.generate(1);
            CourseEventCreateRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getDate()).isNotNull();
            assertThat(dto.getTitle()).isNotBlank();
            assertThat(dto.getDescription()).isNotBlank();
            assertThat(dto.getCourseId()).isEqualTo(10L);
            assertThat(dto.getScheduleId()).isEqualTo(20L);
            assertThat(dto.getInstructorId()).isEqualTo(30L);
        }
    }
}
