/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.util.mock.notification.EmailRecipientFactory.EmailRecipientRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EmailRecipientFactory}.
 */
@DisplayName("EmailRecipientFactory")
class EmailRecipientFactoryTest {

    private EmailRecipientFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmailRecipientFactory(new EmailRecipientDataGenerator());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailableEmailIds(List.of(10L, 20L));

            // When
            List<EmailRecipientRequest> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableEmailIds(List.of(10L));

            // When
            List<EmailRecipientRequest> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when email IDs are not set")
        void shouldThrowIllegalStateException_whenEmailIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(EmailRecipientFactory.ERROR_EMAIL_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign email IDs in round-robin fashion")
        void shouldAssignEmailIds_inRoundRobinFashion() {
            // Given
            factory.setAvailableEmailIds(List.of(100L, 200L));

            // When
            List<EmailRecipientRequest> result = factory.generate(4);

            // Then
            assertThat(result.get(0).emailId()).isEqualTo(100L);
            assertThat(result.get(1).emailId()).isEqualTo(200L);
            assertThat(result.get(2).emailId()).isEqualTo(100L);
            assertThat(result.get(3).emailId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single recipient")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleRecipient() {
            // Given
            factory.setAvailableEmailIds(List.of(10L));

            // When
            List<EmailRecipientRequest> result = factory.generate(1);
            EmailRecipientRequest dto = result.get(0);

            // Then
            assertThat(dto.emailId()).isEqualTo(10L);
            assertThat(dto.recipientEmail()).isNotBlank();
            assertThat(dto.recipientEmail()).contains("@");
        }
    }
}
