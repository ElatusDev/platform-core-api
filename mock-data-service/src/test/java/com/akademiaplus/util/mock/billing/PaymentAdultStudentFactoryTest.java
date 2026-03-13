/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PaymentAdultStudentFactory}.
 */
@DisplayName("PaymentAdultStudentFactory")
class PaymentAdultStudentFactoryTest {

    private PaymentAdultStudentFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PaymentAdultStudentFactory();
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailableMembershipAdultStudentIds(List.of(10L, 20L));

            // When
            List<PaymentAdultStudentCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;
            factory.setAvailableMembershipAdultStudentIds(List.of(10L));

            // When
            List<PaymentAdultStudentCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when membership adult student IDs are not set")
        void shouldThrowIllegalStateException_whenMembershipAdultStudentIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(PaymentAdultStudentFactory.ERROR_MEMBERSHIP_ADULT_STUDENT_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign membership adult student IDs in round-robin fashion")
        void shouldAssignIdsInRoundRobinFashion() {
            // Given
            factory.setAvailableMembershipAdultStudentIds(List.of(100L, 200L));

            // When
            List<PaymentAdultStudentCreationRequestDTO> result = factory.generate(4);

            // Then
            assertThat(result.get(0).getMembershipAdultStudentId()).isEqualTo(100L);
            assertThat(result.get(1).getMembershipAdultStudentId()).isEqualTo(200L);
            assertThat(result.get(2).getMembershipAdultStudentId()).isEqualTo(100L);
            assertThat(result.get(3).getMembershipAdultStudentId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single payment")
        void shouldPopulateAllRequiredFields_whenGeneratingSinglePayment() {
            // Given
            factory.setAvailableMembershipAdultStudentIds(List.of(10L));

            // When
            List<PaymentAdultStudentCreationRequestDTO> result = factory.generate(1);
            PaymentAdultStudentCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getPaymentDate()).isNotNull();
            assertThat(dto.getAmount()).isPositive();
            assertThat(dto.getPaymentMethod()).isNotBlank();
            assertThat(dto.getMembershipAdultStudentId()).isEqualTo(10L);
        }
    }
}
