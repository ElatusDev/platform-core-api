/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MembershipFactory}.
 */
@DisplayName("MembershipFactory")
class MembershipFactoryTest {

    private MembershipFactory factory;

    @BeforeEach
    void setUp() {
        MembershipDataGenerator generator = new MembershipDataGenerator();
        factory = new MembershipFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 4;

            // When
            List<MembershipCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<MembershipCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single membership")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleMembership() {
            // Given
            int singleRecord = 1;

            // When
            List<MembershipCreationRequestDTO> result = factory.generate(singleRecord);
            MembershipCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getMembershipType()).isNotBlank();
            assertThat(dto.getFee()).isPositive();
            assertThat(dto.getDescription()).isNotBlank();
        }
    }
}
