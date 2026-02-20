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

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ScheduleDataGenerator}.
 */
@DisplayName("ScheduleDataGenerator")
class ScheduleDataGeneratorTest {

    private static final List<String> VALID_DAYS = List.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
    );

    private ScheduleDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ScheduleDataGenerator();
    }

    @Nested
    @DisplayName("Schedule day generation")
    class ScheduleDayGeneration {

        @Test
        @DisplayName("Should return a valid day of the week")
        void shouldReturnValidDayOfWeek() {
            // Given
            // generator initialized in setUp

            // When
            String day = generator.scheduleDay();

            // Then
            assertThat(day).isIn(VALID_DAYS);
        }
    }

    @Nested
    @DisplayName("Start time generation")
    class StartTimeGeneration {

        @Test
        @DisplayName("Should return a time between 07:00 and 18:45")
        void shouldReturnTimeBetween7And18() {
            // Given
            // generator initialized in setUp

            // When
            LocalTime startTime = generator.startTime();

            // Then
            assertThat(startTime).isAfterOrEqualTo(LocalTime.of(7, 0));
            assertThat(startTime).isBeforeOrEqualTo(LocalTime.of(18, 45));
        }

        @Test
        @DisplayName("Should return a time with minutes at quarter-hour intervals")
        void shouldReturnTimeWithQuarterHourMinutes() {
            // Given
            List<Integer> validMinutes = List.of(0, 15, 30, 45);

            // When
            LocalTime startTime = generator.startTime();

            // Then
            assertThat(startTime.getMinute()).isIn(validMinutes);
        }
    }

    @Nested
    @DisplayName("End time generation")
    class EndTimeGeneration {

        @Test
        @DisplayName("Should return a time after the start time")
        void shouldReturnTimeAfterStartTime() {
            // Given
            LocalTime startTime = LocalTime.of(10, 0);

            // When
            LocalTime endTime = generator.endTime(startTime);

            // Then
            assertThat(endTime).isAfter(startTime);
        }

        @Test
        @DisplayName("Should return an end time 45 to 120 minutes after start")
        void shouldReturnEndTimeBetween45And120MinutesAfterStart() {
            // Given
            LocalTime startTime = LocalTime.of(10, 0);

            // When
            LocalTime endTime = generator.endTime(startTime);

            // Then
            assertThat(endTime).isAfterOrEqualTo(startTime.plusMinutes(45));
            assertThat(endTime).isBeforeOrEqualTo(startTime.plusMinutes(120));
        }
    }
}
