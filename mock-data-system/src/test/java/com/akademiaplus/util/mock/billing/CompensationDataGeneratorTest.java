/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CompensationDataGenerator}.
 */
@DisplayName("CompensationDataGenerator")
class CompensationDataGeneratorTest {

    private static final List<String> VALID_TYPES = List.of(
            "hourly", "salary", "commission", "contract", "bonus"
    );

    private CompensationDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CompensationDataGenerator();
    }

    @Nested
    @DisplayName("Compensation type generation")
    class CompensationTypeGeneration {

        @Test
        @DisplayName("Should return a valid compensation type")
        void shouldReturnValidCompensationType() {
            // Given
            // generator initialized in setUp

            // When
            String type = generator.compensationType();

            // Then
            assertThat(type).isIn(VALID_TYPES);
        }
    }

    @Nested
    @DisplayName("Amount generation")
    class AmountGeneration {

        @Test
        @DisplayName("Should return an amount between 5000 and 49999")
        void shouldReturnAmountInRange() {
            // Given
            // generator initialized in setUp

            // When
            double amount = generator.amount();

            // Then
            assertThat(amount).isBetween(5000.0, 49999.0);
        }
    }
}
