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
    @DisplayName("Sale item count generation")
    class SaleItemCountGeneration {

        @Test
        @DisplayName("Should return a sale item count within configured range")
        void shouldReturnSaleItemCountInRange() {
            // Given
            // generator initialized in setUp

            // When
            int count = generator.saleItemCount();

            // Then
            assertThat(count).isBetween(
                    StoreTransactionDataGenerator.MIN_SALE_ITEM_COUNT,
                    StoreTransactionDataGenerator.MAX_SALE_ITEM_COUNT);
        }
    }

    @Nested
    @DisplayName("Sale item store product ID generation")
    class SaleItemStoreProductIdGeneration {

        @Test
        @DisplayName("Should return a store product ID within configured range")
        void shouldReturnStoreProductIdInRange() {
            // Given
            // generator initialized in setUp

            // When
            Long productId = generator.saleItemStoreProductId();

            // Then
            assertThat(productId).isBetween(
                    StoreTransactionDataGenerator.MIN_STORE_PRODUCT_ID,
                    StoreTransactionDataGenerator.MAX_STORE_PRODUCT_ID);
        }
    }

    @Nested
    @DisplayName("Sale item quantity generation")
    class SaleItemQuantityGeneration {

        @Test
        @DisplayName("Should return a sale item quantity within configured range")
        void shouldReturnSaleItemQuantityInRange() {
            // Given
            // generator initialized in setUp

            // When
            int quantity = generator.saleItemQuantity();

            // Then
            assertThat(quantity).isBetween(
                    StoreTransactionDataGenerator.MIN_SALE_ITEM_QUANTITY,
                    StoreTransactionDataGenerator.MAX_SALE_ITEM_QUANTITY);
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

    @Nested
    @DisplayName("Total amount generation")
    class TotalAmountGeneration {

        @Test
        @DisplayName("Should return a total amount within configured range")
        void shouldReturnTotalAmountInRange() {
            // Given
            // generator initialized in setUp

            // When
            Double amount = generator.totalAmount();

            // Then
            assertThat(amount).isBetween(
                    StoreTransactionDataGenerator.MIN_TOTAL_AMOUNT,
                    StoreTransactionDataGenerator.MAX_TOTAL_AMOUNT);
        }
    }
}
