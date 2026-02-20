/*
 * Copyright (c) 2025 ElatusDev
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CourseDataGenerator}.
 */
@DisplayName("CourseDataGenerator")
class CourseDataGeneratorTest {

    private CourseDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CourseDataGenerator();
    }

    @Nested
    @DisplayName("Course name generation")
    class CourseNameGeneration {

        @Test
        @DisplayName("Should return a non-blank course name")
        void shouldReturnNonBlankCourseName() {
            // Given
            // generator initialized in setUp

            // When
            String name = generator.courseName();

            // Then
            assertThat(name).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Course description generation")
    class CourseDescriptionGeneration {

        @Test
        @DisplayName("Should return a non-blank description")
        void shouldReturnNonBlankDescription() {
            // Given
            // generator initialized in setUp

            // When
            String description = generator.courseDescription();

            // Then
            assertThat(description).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Max capacity generation")
    class MaxCapacityGeneration {

        @Test
        @DisplayName("Should return a capacity between 10 and 49")
        void shouldReturnCapacityInRange() {
            // Given
            // generator initialized in setUp

            // When
            int capacity = generator.maxCapacity();

            // Then
            assertThat(capacity).isBetween(10, 49);
        }
    }
}
