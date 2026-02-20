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
 * Unit tests for {@link MembershipDataGenerator}.
 */
@DisplayName("MembershipDataGenerator")
class MembershipDataGeneratorTest {

    private static final List<String> VALID_TYPES = List.of(
            "monthly", "quarterly", "semiannual", "annual"
    );

    private MembershipDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MembershipDataGenerator();
    }

    @Nested
    @DisplayName("Membership type generation")
    class MembershipTypeGeneration {

        @Test
        @DisplayName("Should return a valid membership type")
        void shouldReturnValidMembershipType() {
            // Given
            // generator initialized in setUp

            // When
            String type = generator.membershipType();

            // Then
            assertThat(type).isIn(VALID_TYPES);
        }
    }

    @Nested
    @DisplayName("Fee generation")
    class FeeGeneration {

        @Test
        @DisplayName("Should return a fee between 500 and 4999")
        void shouldReturnFeeInRange() {
            // Given
            // generator initialized in setUp

            // When
            double fee = generator.fee();

            // Then
            assertThat(fee).isBetween(500.0, 4999.0);
        }
    }

    @Nested
    @DisplayName("Description generation")
    class DescriptionGeneration {

        @Test
        @DisplayName("Should return a non-blank description")
        void shouldReturnNonBlankDescription() {
            // Given
            // generator initialized in setUp

            // When
            String description = generator.description();

            // Then
            assertThat(description).isNotBlank();
        }
    }
}
