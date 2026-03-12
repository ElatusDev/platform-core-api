/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.util.mock.notification.NotificationDeliveryFactory.NotificationDeliveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link NotificationDeliveryFactory}.
 */
@DisplayName("NotificationDeliveryFactory")
class NotificationDeliveryFactoryTest {

    private NotificationDeliveryFactory factory;

    @BeforeEach
    void setUp() {
        factory = new NotificationDeliveryFactory(new NotificationDeliveryDataGenerator());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailableNotificationIds(List.of(10L, 20L));

            // When
            List<NotificationDeliveryRequest> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableNotificationIds(List.of(10L));

            // When
            List<NotificationDeliveryRequest> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when notification IDs are not set")
        void shouldThrowIllegalStateException_whenNotificationIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(NotificationDeliveryFactory.ERROR_NOTIFICATION_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign notification IDs in round-robin fashion")
        void shouldAssignNotificationIds_inRoundRobinFashion() {
            // Given
            factory.setAvailableNotificationIds(List.of(100L, 200L));

            // When
            List<NotificationDeliveryRequest> result = factory.generate(4);

            // Then
            assertThat(result.get(0).notificationId()).isEqualTo(100L);
            assertThat(result.get(1).notificationId()).isEqualTo(200L);
            assertThat(result.get(2).notificationId()).isEqualTo(100L);
            assertThat(result.get(3).notificationId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single delivery")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleDelivery() {
            // Given
            factory.setAvailableNotificationIds(List.of(10L));

            // When
            List<NotificationDeliveryRequest> result = factory.generate(1);
            NotificationDeliveryRequest dto = result.get(0);

            // Then
            assertThat(dto.notificationId()).isEqualTo(10L);
            assertThat(dto.channel()).isNotNull();
            assertThat(dto.recipientIdentifier()).isNotBlank();
            assertThat(dto.status()).isNotNull();
            assertThat(dto.sentAt()).isNotNull();
            assertThat(dto.retryCount()).isGreaterThanOrEqualTo(0);
            assertThat(dto.externalId()).isNotBlank();
        }
    }
}
