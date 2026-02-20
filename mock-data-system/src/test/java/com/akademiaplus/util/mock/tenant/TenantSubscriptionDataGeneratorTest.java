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
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TenantSubscriptionDataGenerator}.
 */
@DisplayName("TenantSubscriptionDataGenerator")
class TenantSubscriptionDataGeneratorTest {

    private static final List<SubscriptionCreateRequestDTO.TypeEnum> VALID_TYPES = List.of(
            SubscriptionCreateRequestDTO.TypeEnum.BASIC,
            SubscriptionCreateRequestDTO.TypeEnum.STANDARD,
            SubscriptionCreateRequestDTO.TypeEnum.PREMIUM,
            SubscriptionCreateRequestDTO.TypeEnum.ENTERPRISE
    );

    private TenantSubscriptionDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TenantSubscriptionDataGenerator();
    }

    @Nested
    @DisplayName("Subscription type generation")
    class SubscriptionTypeGeneration {

        @Test
        @DisplayName("Should return a valid subscription type enum value")
        void shouldReturnValidSubscriptionType() {
            // Given
            // generator initialized in setUp

            // When
            SubscriptionCreateRequestDTO.TypeEnum type = generator.type();

            // Then
            assertThat(type).isIn(VALID_TYPES);
        }
    }

    @Nested
    @DisplayName("Max users generation")
    class MaxUsersGeneration {

        @Test
        @DisplayName("Should return present JsonNullable with value between 10 and 499")
        void shouldReturnPresentJsonNullable_withValueInRange() {
            // Given
            // generator initialized in setUp

            // When
            JsonNullable<Integer> maxUsers = generator.maxUsers();

            // Then
            assertThat(maxUsers.isPresent()).isTrue();
            assertThat(maxUsers.get()).isBetween(10, 499);
        }
    }

    @Nested
    @DisplayName("Billing date generation")
    class BillingDateGeneration {

        @Test
        @DisplayName("Should return a future date within the next 30 days")
        void shouldReturnFutureDateWithinThirtyDays() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate billingDate = generator.billingDate();

            // Then
            assertThat(billingDate).isAfter(today);
            assertThat(billingDate).isBeforeOrEqualTo(today.plusDays(30));
        }
    }

    @Nested
    @DisplayName("Rate per student generation")
    class RatePerStudentGeneration {

        @Test
        @DisplayName("Should return a rate between 50 and 499")
        void shouldReturnRateInRange() {
            // Given
            // generator initialized in setUp

            // When
            double rate = generator.ratePerStudent();

            // Then
            assertThat(rate).isBetween(50.0, 499.0);
        }
    }
}
