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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StoreProductDataGenerator}.
 */
@DisplayName("StoreProductDataGenerator")
class StoreProductDataGeneratorTest {

    private StoreProductDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new StoreProductDataGenerator();
    }

    @Nested
    @DisplayName("Product name generation")
    class ProductNameGeneration {

        @Test
        @DisplayName("Should return a non-blank product name")
        void shouldReturnNonBlankProductName() {
            // Given
            // generator initialized in setUp

            // When
            String name = generator.name();

            // Then
            assertThat(name).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Product description generation")
    class ProductDescriptionGeneration {

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

    @Nested
    @DisplayName("Price generation")
    class PriceGeneration {

        @Test
        @DisplayName("Should return a price between 10 and 4999")
        void shouldReturnPriceInRange() {
            // Given
            // generator initialized in setUp

            // When
            double price = generator.price();

            // Then
            assertThat(price).isBetween(10.0, 4999.0);
        }
    }

    @Nested
    @DisplayName("Stock quantity generation")
    class StockQuantityGeneration {

        @Test
        @DisplayName("Should return a stock quantity between 1 and 199")
        void shouldReturnStockQuantityInRange() {
            // Given
            // generator initialized in setUp

            // When
            int quantity = generator.stockQuantity();

            // Then
            assertThat(quantity).isBetween(1, 199);
        }
    }
}
