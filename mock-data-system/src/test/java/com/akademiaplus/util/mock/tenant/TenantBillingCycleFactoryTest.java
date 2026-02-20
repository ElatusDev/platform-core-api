/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantBillingCycleFactory}.
 */
@DisplayName("TenantBillingCycleFactory")
class TenantBillingCycleFactoryTest {

    private TenantBillingCycleFactory factory;

    @BeforeEach
    void setUp() {
        TenantBillingCycleDataGenerator generator = new TenantBillingCycleDataGenerator();
        factory = new TenantBillingCycleFactory(generator);
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
            List<BillingCycleCreateRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<BillingCycleCreateRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single billing cycle")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleBillingCycle() {
            // Given
            int singleRecord = 1;

            // When
            List<BillingCycleCreateRequestDTO> result = factory.generate(singleRecord);
            BillingCycleCreateRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getBillingMonth()).matches("\\d{4}-\\d{2}");
            assertThat(dto.getCalculationDate()).isNotNull();
            assertThat(dto.getUserCount()).isPositive();
            assertThat(dto.getTotalAmount()).isPositive();
            assertThat(dto.getNotes()).isNotBlank();
        }
    }
}
