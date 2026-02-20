/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import com.akademiaplus.util.mock.billing.CardPaymentInfoFactory.CardPaymentInfoRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CardPaymentInfoFactory}.
 */
@DisplayName("CardPaymentInfoFactory")
class CardPaymentInfoFactoryTest {

    private CardPaymentInfoFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CardPaymentInfoFactory(new CardPaymentInfoDataGenerator());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailablePaymentAdultStudentIds(List.of(10L, 20L));

            // When
            List<CardPaymentInfoRequest> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailablePaymentAdultStudentIds(List.of(10L));

            // When
            List<CardPaymentInfoRequest> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when payment IDs are not set")
        void shouldThrowIllegalStateException_whenPaymentIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(CardPaymentInfoFactory.ERROR_PAYMENT_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign payment IDs in round-robin fashion")
        void shouldAssignPaymentIds_inRoundRobinFashion() {
            // Given
            factory.setAvailablePaymentAdultStudentIds(List.of(100L, 200L));

            // When
            List<CardPaymentInfoRequest> result = factory.generate(4);

            // Then
            assertThat(result.get(0).paymentId()).isEqualTo(100L);
            assertThat(result.get(1).paymentId()).isEqualTo(200L);
            assertThat(result.get(2).paymentId()).isEqualTo(100L);
            assertThat(result.get(3).paymentId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single record")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleRecord() {
            // Given
            factory.setAvailablePaymentAdultStudentIds(List.of(10L));

            // When
            List<CardPaymentInfoRequest> result = factory.generate(1);
            CardPaymentInfoRequest dto = result.get(0);

            // Then
            assertThat(dto.paymentId()).isEqualTo(10L);
            assertThat(dto.token()).isNotBlank();
            assertThat(dto.cardType()).isNotBlank();
        }
    }
}
