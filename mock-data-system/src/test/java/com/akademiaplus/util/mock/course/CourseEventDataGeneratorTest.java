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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CourseEventDataGenerator}.
 */
@DisplayName("CourseEventDataGenerator")
class CourseEventDataGeneratorTest {

    private CourseEventDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CourseEventDataGenerator();
    }

    @Nested
    @DisplayName("Event date generation")
    class EventDateGeneration {

        @Test
        @DisplayName("Should return a date within 30 days in the past to 60 days in the future")
        void shouldReturnDateWithinExpectedRange() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate eventDate = generator.eventDate();

            // Then
            assertThat(eventDate).isAfterOrEqualTo(today.minusDays(30));
            assertThat(eventDate).isBeforeOrEqualTo(today.plusDays(60));
        }
    }

    @Nested
    @DisplayName("Event title generation")
    class EventTitleGeneration {

        @Test
        @DisplayName("Should return a non-blank title containing a hyphen separator")
        void shouldReturnNonBlankTitleWithHyphen() {
            // Given
            // generator initialized in setUp

            // When
            String title = generator.eventTitle();

            // Then
            assertThat(title).isNotBlank();
            assertThat(title).contains(" - ");
        }
    }

    @Nested
    @DisplayName("Event description generation")
    class EventDescriptionGeneration {

        @Test
        @DisplayName("Should return a non-blank description")
        void shouldReturnNonBlankDescription() {
            // Given
            // generator initialized in setUp

            // When
            String description = generator.eventDescription();

            // Then
            assertThat(description).isNotBlank();
        }
    }
}
