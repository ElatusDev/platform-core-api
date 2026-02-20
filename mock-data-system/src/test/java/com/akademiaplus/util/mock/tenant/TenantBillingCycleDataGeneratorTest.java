/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantBillingCycleDataGenerator}.
 */
@DisplayName("TenantBillingCycleDataGenerator")
class TenantBillingCycleDataGeneratorTest {

    private TenantBillingCycleDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TenantBillingCycleDataGenerator();
    }

    @Nested
    @DisplayName("Billing month generation")
    class BillingMonthGeneration {

        @Test
        @DisplayName("Should return a string in yyyy-MM format")
        void shouldReturnStringInYearMonthFormat() {
            // Given
            // generator initialized in setUp

            // When
            String billingMonth = generator.billingMonth();

            // Then
            assertThat(billingMonth).matches("\\d{4}-\\d{2}");
        }

        @Test
        @DisplayName("Should return a month within the last 12 months")
        void shouldReturnMonthWithinLastTwelveMonths() {
            // Given
            String oldest = LocalDate.now().minusMonths(12).format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
            String newest = LocalDate.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));

            // When
            String billingMonth = generator.billingMonth();

            // Then
            assertThat(billingMonth).isGreaterThanOrEqualTo(oldest);
            assertThat(billingMonth).isLessThanOrEqualTo(newest);
        }
    }

    @Nested
    @DisplayName("Calculation date generation")
    class CalculationDateGeneration {

        @Test
        @DisplayName("Should return a date within the last 30 days")
        void shouldReturnDateWithinLastThirtyDays() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate calculationDate = generator.calculationDate();

            // Then
            assertThat(calculationDate).isAfterOrEqualTo(today.minusDays(30));
            assertThat(calculationDate).isBeforeOrEqualTo(today);
        }
    }

    @Nested
    @DisplayName("User count generation")
    class UserCountGeneration {

        @Test
        @DisplayName("Should return a count between 5 and 199")
        void shouldReturnCountInRange() {
            // Given
            // generator initialized in setUp

            // When
            int userCount = generator.userCount();

            // Then
            assertThat(userCount).isBetween(5, 199);
        }
    }

    @Nested
    @DisplayName("Notes generation")
    class NotesGeneration {

        @Test
        @DisplayName("Should return non-blank notes")
        void shouldReturnNonBlankNotes() {
            // Given
            // generator initialized in setUp

            // When
            String notes = generator.notes();

            // Then
            assertThat(notes).isNotBlank();
        }
    }
}
