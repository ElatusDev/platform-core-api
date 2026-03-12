/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.notifications.DeliveryChannel;
import com.akademiaplus.notifications.DeliveryStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NotificationDeliveryDataGenerator}.
 */
@DisplayName("NotificationDeliveryDataGenerator")
class NotificationDeliveryDataGeneratorTest {

    private NotificationDeliveryDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new NotificationDeliveryDataGenerator();
    }

    @Nested
    @DisplayName("channel()")
    class Channel {

        @Test
        @DisplayName("Should generate valid delivery channel")
        void shouldGenerateValidDeliveryChannel() {
            // Given & When
            DeliveryChannel channel = generator.channel();

            // Then
            assertThat(channel).isNotNull();
            assertThat(channel).isIn((Object[]) DeliveryChannel.values());
        }
    }

    @Nested
    @DisplayName("status()")
    class Status {

        @Test
        @DisplayName("Should generate valid delivery status")
        void shouldGenerateValidDeliveryStatus() {
            // Given & When
            DeliveryStatus status = generator.status();

            // Then
            assertThat(status).isNotNull();
            assertThat(status).isIn((Object[]) DeliveryStatus.values());
        }
    }

    @Nested
    @DisplayName("recipientIdentifier()")
    class RecipientIdentifier {

        @Test
        @DisplayName("Should generate non-blank recipient identifier")
        void shouldGenerateNonBlankRecipientIdentifier() {
            // Given & When
            String recipient = generator.recipientIdentifier();

            // Then
            assertThat(recipient).isNotBlank();
        }
    }

    @Nested
    @DisplayName("sentAt()")
    class SentAt {

        @Test
        @DisplayName("Should generate sentAt within last 30 days")
        void shouldGenerateSentAt_withinLast30Days() {
            // Given & When
            LocalDateTime sentAt = generator.sentAt();

            // Then
            assertThat(sentAt).isNotNull();
            assertThat(sentAt).isAfterOrEqualTo(LocalDateTime.now().minusDays(31));
            assertThat(sentAt).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }

    @Nested
    @DisplayName("retryCount()")
    class RetryCount {

        @Test
        @DisplayName("Should generate retry count between 0 and 3")
        void shouldGenerateRetryCount_between0And3() {
            // Given & When
            int retryCount = generator.retryCount();

            // Then
            assertThat(retryCount).isBetween(0, 3);
        }
    }

    @Nested
    @DisplayName("externalId()")
    class ExternalId {

        @Test
        @DisplayName("Should generate non-blank external ID with ext_ prefix")
        void shouldGenerateNonBlankExternalId_withExtPrefix() {
            // Given & When
            String externalId = generator.externalId();

            // Then
            assertThat(externalId).isNotBlank();
            assertThat(externalId).startsWith("ext_");
        }
    }
}
