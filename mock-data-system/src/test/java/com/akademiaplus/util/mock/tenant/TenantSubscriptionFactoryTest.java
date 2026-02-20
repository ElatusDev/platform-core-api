/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantSubscriptionFactory}.
 */
@DisplayName("TenantSubscriptionFactory")
class TenantSubscriptionFactoryTest {

    private TenantSubscriptionFactory factory;

    @BeforeEach
    void setUp() {
        TenantSubscriptionDataGenerator generator = new TenantSubscriptionDataGenerator();
        factory = new TenantSubscriptionFactory(generator);
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
            List<SubscriptionCreateRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<SubscriptionCreateRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single subscription")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleSubscription() {
            // Given
            int singleRecord = 1;

            // When
            List<SubscriptionCreateRequestDTO> result = factory.generate(singleRecord);
            SubscriptionCreateRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getType()).isNotNull();
            assertThat(dto.getMaxUsers().isPresent()).isTrue();
            assertThat(dto.getBillingDate()).isNotNull();
            assertThat(dto.getRatePerStudent()).isPositive();
        }
    }
}
