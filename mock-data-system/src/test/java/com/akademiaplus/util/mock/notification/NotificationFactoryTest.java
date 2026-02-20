/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import openapi.akademiaplus.domain.notification.system.dto.NotificationCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationFactory}.
 */
@DisplayName("NotificationFactory")
class NotificationFactoryTest {

    private NotificationFactory factory;

    @BeforeEach
    void setUp() {
        NotificationDataGenerator generator = new NotificationDataGenerator();
        factory = new NotificationFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 5;

            // When
            List<NotificationCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<NotificationCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single notification")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleNotification() {
            // Given
            int singleRecord = 1;

            // When
            List<NotificationCreationRequestDTO> result = factory.generate(singleRecord);
            NotificationCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getTitle()).isNotBlank();
            assertThat(dto.getContent()).isNotBlank();
            assertThat(dto.getType()).isNotBlank();
            assertThat(dto.getPriority()).isNotBlank();
            assertThat(dto.getScheduledAt()).isNotNull();
            assertThat(dto.getExpiresAt()).isNotNull();
            assertThat(dto.getExpiresAt()).isAfter(dto.getScheduledAt());
        }
    }
}
