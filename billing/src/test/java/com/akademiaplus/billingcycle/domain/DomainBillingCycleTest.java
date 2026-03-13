/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billingcycle.domain;

import com.akademiaplus.billingcycle.domain.exception.InvalidBillingTransitionException;
import com.akademiaplus.tenancy.BillingStatus;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DomainBillingCycle}.
 *
 * <p>Plain JUnit — no Spring context required.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("DomainBillingCycle")
class DomainBillingCycleTest {

    private final DomainBillingCycle domainBillingCycle = new DomainBillingCycle();

    private TenantBillingCycleDataModel buildCycle(BillingStatus status) {
        TenantBillingCycleDataModel cycle = new TenantBillingCycleDataModel();
        cycle.setTenantId(1L);
        cycle.setTenantBillingCycleId(10L);
        cycle.setBillingStatus(status);
        return cycle;
    }

    @Nested
    @DisplayName("validateTransition — valid transitions")
    class ValidTransitions {

        @Test
        @DisplayName("Should allow PENDING → BILLED")
        void shouldAllowTransition_whenPendingToBilled() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.PENDING);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.BILLED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow PENDING → CANCELLED")
        void shouldAllowTransition_whenPendingToCancelled() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.PENDING);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.CANCELLED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow BILLED → PAID")
        void shouldAllowTransition_whenBilledToPaid() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.BILLED);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.PAID))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow BILLED → FAILED")
        void shouldAllowTransition_whenBilledToFailed() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.BILLED);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.FAILED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow FAILED → BILLED (retry)")
        void shouldAllowTransition_whenFailedToBilled() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.FAILED);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.BILLED))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow FAILED → CANCELLED (abandon)")
        void shouldAllowTransition_whenFailedToCancelled() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.FAILED);

            // When / Then
            assertThatCode(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.CANCELLED))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateTransition — invalid transitions")
    class InvalidTransitions {

        @Test
        @DisplayName("Should reject PENDING → PAID (must go through BILLED)")
        void shouldThrowInvalidBillingTransitionException_whenPendingToPaid() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.PENDING);

            // When / Then
            assertThatThrownBy(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.PAID))
                    .isInstanceOf(InvalidBillingTransitionException.class)
                    .hasMessageContaining(InvalidBillingTransitionException.ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should reject PENDING → FAILED")
        void shouldThrowInvalidBillingTransitionException_whenPendingToFailed() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.PENDING);

            // When / Then
            assertThatThrownBy(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.FAILED))
                    .isInstanceOf(InvalidBillingTransitionException.class)
                    .hasMessageContaining(InvalidBillingTransitionException.ERROR_MESSAGE);
        }

        @Test
        @DisplayName("Should reject BILLED → PENDING (no backwards)")
        void shouldThrowInvalidBillingTransitionException_whenBilledToPending() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.BILLED);

            // When / Then
            assertThatThrownBy(() -> domainBillingCycle.get(cycle)
                    .validateTransition(BillingStatus.PENDING))
                    .isInstanceOf(InvalidBillingTransitionException.class)
                    .hasMessageContaining(InvalidBillingTransitionException.ERROR_MESSAGE);
        }
    }

    @Nested
    @DisplayName("validateTransition — terminal states")
    class TerminalStates {

        @Test
        @DisplayName("Should reject any transition from PAID")
        void shouldThrowInvalidBillingTransitionException_whenPaidToAnyState() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.PAID);

            // When / Then — try all possible targets
            for (BillingStatus target : BillingStatus.values()) {
                if (target == BillingStatus.PAID) continue;
                assertThatThrownBy(() -> domainBillingCycle.get(cycle)
                        .validateTransition(target))
                        .isInstanceOf(InvalidBillingTransitionException.class)
                        .hasMessageContaining(InvalidBillingTransitionException.ERROR_MESSAGE);
            }
        }

        @Test
        @DisplayName("Should reject any transition from CANCELLED")
        void shouldThrowInvalidBillingTransitionException_whenCancelledToAnyState() {
            // Given
            TenantBillingCycleDataModel cycle = buildCycle(BillingStatus.CANCELLED);

            // When / Then — try all possible targets
            for (BillingStatus target : BillingStatus.values()) {
                if (target == BillingStatus.CANCELLED) continue;
                assertThatThrownBy(() -> domainBillingCycle.get(cycle)
                        .validateTransition(target))
                        .isInstanceOf(InvalidBillingTransitionException.class)
                        .hasMessageContaining(InvalidBillingTransitionException.ERROR_MESSAGE);
            }
        }
    }
}
