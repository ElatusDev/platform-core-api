/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StoreTransactionDataGenerator}.
 */
@DisplayName("StoreTransactionDataGenerator")
class StoreTransactionDataGeneratorTest {

    private static final List<String> VALID_TRANSACTION_TYPES = List.of(
            "SALE", "REFUND", "EXCHANGE"
    );

    private static final List<String> VALID_PAYMENT_METHODS = List.of(
            "CASH", "CREDIT_CARD", "DEBIT_CARD", "TRANSFER"
    );

    private StoreTransactionDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StoreTransactionDataGenerator();
    }

    @Nested
    @DisplayName("Transaction type generation")
    class TransactionTypeGeneration {

        @Test
        @DisplayName("Should return a valid transaction type")
        void shouldReturnValidTransactionType() {
            // Given
            // generator initialized in setUp

            // When
            String type = generator.transactionType();

            // Then
            assertThat(type).isIn(VALID_TRANSACTION_TYPES);
        }
    }

    @Nested
    @DisplayName("Total amount generation")
    class TotalAmountGeneration {

        @Test
        @DisplayName("Should return a total amount between 50 and 9999")
        void shouldReturnTotalAmountInRange() {
            // Given
            // generator initialized in setUp

            // When
            double amount = generator.totalAmount();

            // Then
            assertThat(amount).isBetween(50.0, 9999.0);
        }
    }

    @Nested
    @DisplayName("Payment method generation")
    class PaymentMethodGeneration {

        @Test
        @DisplayName("Should return a valid payment method")
        void shouldReturnValidPaymentMethod() {
            // Given
            // generator initialized in setUp

            // When
            String method = generator.paymentMethod();

            // Then
            assertThat(method).isIn(VALID_PAYMENT_METHODS);
        }
    }
}
