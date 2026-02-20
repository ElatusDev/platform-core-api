/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ScheduleFactory}.
 */
@DisplayName("ScheduleFactory")
class ScheduleFactoryTest {

    private ScheduleFactory factory;

    @BeforeEach
    void setUp() {
        ScheduleDataGenerator generator = new ScheduleDataGenerator();
        factory = new ScheduleFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and course IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndCourseIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailableCourseIds(List.of(10L, 20L));

            // When
            List<ScheduleCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;
            factory.setAvailableCourseIds(List.of(10L));

            // When
            List<ScheduleCreationRequestDTO> result = factory.generate(zeroCount);

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
            int count = 3;

            // When & Then
            assertThatThrownBy(() -> factory.generate(count))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("availableCourseIds must be set");
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign course IDs in round-robin fashion")
        void shouldAssignCourseIdsInRoundRobinFashion() {
            // Given
            List<Long> courseIds = List.of(100L, 200L);
            factory.setAvailableCourseIds(courseIds);

            // When
            List<ScheduleCreationRequestDTO> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getCourseId()).isEqualTo(100L);
            assertThat(result.get(1).getCourseId()).isEqualTo(200L);
            assertThat(result.get(2).getCourseId()).isEqualTo(100L);
            assertThat(result.get(3).getCourseId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single schedule")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleSchedule() {
            // Given
            factory.setAvailableCourseIds(List.of(10L));
            int singleRecord = 1;

            // When
            List<ScheduleCreationRequestDTO> result = factory.generate(singleRecord);
            ScheduleCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getScheduleDay()).isNotBlank();
            assertThat(dto.getStartTime()).isNotBlank();
            assertThat(dto.getEndTime()).isNotBlank();
            assertThat(dto.getCourseId()).isEqualTo(10L);
        }
    }
}
