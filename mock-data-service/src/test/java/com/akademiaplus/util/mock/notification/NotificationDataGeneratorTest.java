/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationDataGenerator}.
 */
@DisplayName("NotificationDataGenerator")
class NotificationDataGeneratorTest {

    private static final List<String> VALID_TYPES = List.of(
            "COURSE_REMINDER", "PAYMENT_DUE", "ENROLLMENT_CONFIRMATION",
            "SCHEDULE_CHANGE", "SYSTEM_MAINTENANCE", "PROMOTIONAL",
            "ANNOUNCEMENT", "ASSIGNMENT_REMINDER", "GRADE_NOTIFICATION"
    );

    private static final List<String> VALID_PRIORITIES = List.of(
            "LOW", "NORMAL", "HIGH", "URGENT"
    );

    private NotificationDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new NotificationDataGenerator();
    }

    @Nested
    @DisplayName("Title generation")
    class TitleGeneration {

        @Test
        @DisplayName("Should return a non-blank title")
        void shouldReturnNonBlankTitle() {
            // Given
            // generator initialized in setUp

            // When
            String title = generator.title();

            // Then
            assertThat(title).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Content generation")
    class ContentGeneration {

        @Test
        @DisplayName("Should return non-blank content")
        void shouldReturnNonBlankContent() {
            // Given
            // generator initialized in setUp

            // When
            String content = generator.content();

            // Then
            assertThat(content).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Type generation")
    class TypeGeneration {

        @Test
        @DisplayName("Should return a valid notification type")
        void shouldReturnValidNotificationType() {
            // Given
            // generator initialized in setUp

            // When
            String type = generator.type();

            // Then
            assertThat(type).isIn(VALID_TYPES);
        }
    }

    @Nested
    @DisplayName("Priority generation")
    class PriorityGeneration {

        @Test
        @DisplayName("Should return a valid priority")
        void shouldReturnValidPriority() {
            // Given
            // generator initialized in setUp

            // When
            String priority = generator.priority();

            // Then
            assertThat(priority).isIn(VALID_PRIORITIES);
        }
    }

    @Nested
    @DisplayName("Scheduled-at generation")
    class ScheduledAtGeneration {

        @Test
        @DisplayName("Should return a future datetime within the next 30 days")
        void shouldReturnFutureDatetimeWithinThirtyDays() {
            // Given
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

            // When
            OffsetDateTime scheduledAt = generator.scheduledAt();

            // Then
            assertThat(scheduledAt).isAfter(now);
            assertThat(scheduledAt).isBefore(now.plusDays(31));
        }
    }

    @Nested
    @DisplayName("Expires-at generation")
    class ExpiresAtGeneration {

        @Test
        @DisplayName("Should return a datetime after scheduledAt")
        void shouldReturnDatetimeAfterScheduledAt() {
            // Given
            OffsetDateTime scheduledAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);

            // When
            OffsetDateTime expiresAt = generator.expiresAt(scheduledAt);

            // Then
            assertThat(expiresAt).isAfter(scheduledAt);
        }

        @Test
        @DisplayName("Should return expiry 7 to 90 days after scheduledAt")
        void shouldReturnExpiryBetween7And90DaysAfterScheduledAt() {
            // Given
            OffsetDateTime scheduledAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(5);

            // When
            OffsetDateTime expiresAt = generator.expiresAt(scheduledAt);

            // Then
            assertThat(expiresAt).isAfterOrEqualTo(scheduledAt.plusDays(7));
            assertThat(expiresAt).isBeforeOrEqualTo(scheduledAt.plusDays(90));
        }
    }
}
