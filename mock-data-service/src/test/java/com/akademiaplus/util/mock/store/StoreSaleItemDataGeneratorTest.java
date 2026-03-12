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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StoreSaleItemDataGenerator}.
 */
@DisplayName("StoreSaleItemDataGenerator")
class StoreSaleItemDataGeneratorTest {

    private StoreSaleItemDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StoreSaleItemDataGenerator();
    }

    @Nested
    @DisplayName("quantity()")
    class Quantity {

        @Test
        @DisplayName("Should generate quantity within valid range")
        void shouldGenerateQuantity_withinValidRange() {
            // Given & When
            int quantity = generator.quantity();

            // Then
            assertThat(quantity).isBetween(
                    StoreSaleItemDataGenerator.MIN_QUANTITY,
                    StoreSaleItemDataGenerator.MAX_QUANTITY);
        }
    }

    @Nested
    @DisplayName("unitPriceAtSale()")
    class UnitPriceAtSale {

        @Test
        @DisplayName("Should generate positive price with scale 2")
        void shouldGeneratePositivePrice_withScale2() {
            // Given & When
            BigDecimal price = generator.unitPriceAtSale();

            // Then
            assertThat(price).isPositive();
            assertThat(price.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should generate price within valid range")
        void shouldGeneratePrice_withinValidRange() {
            // Given & When
            BigDecimal price = generator.unitPriceAtSale();

            // Then
            assertThat(price).isGreaterThanOrEqualTo(BigDecimal.valueOf(StoreSaleItemDataGenerator.MIN_PRICE));
            assertThat(price).isLessThanOrEqualTo(BigDecimal.valueOf(StoreSaleItemDataGenerator.MAX_PRICE));
        }
    }
}
