/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.store;

import com.akademiaplus.util.mock.store.StoreSaleItemFactory.StoreSaleItemRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StoreSaleItemFactory}.
 */
@DisplayName("StoreSaleItemFactory")
class StoreSaleItemFactoryTest {

    private StoreSaleItemFactory factory;

    @BeforeEach
    void setUp() {
        factory = new StoreSaleItemFactory(new StoreSaleItemDataGenerator());
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number and IDs set")
        void shouldGenerateExactCount_whenGivenPositiveNumberAndIdsSet() {
            // Given
            int expectedCount = 4;
            factory.setAvailableStoreTransactionIds(List.of(10L, 20L));
            factory.setAvailableStoreProductIds(List.of(30L, 40L));

            // When
            List<StoreSaleItemRequest> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableStoreTransactionIds(List.of(10L));
            factory.setAvailableStoreProductIds(List.of(30L));

            // When
            List<StoreSaleItemRequest> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Stateful constraint validation")
    class StatefulConstraintValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when transaction IDs are not set")
        void shouldThrowIllegalStateException_whenTransactionIdsNotSet() {
            // Given & When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(StoreSaleItemFactory.ERROR_TRANSACTION_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when product IDs are not set")
        void shouldThrowIllegalStateException_whenProductIdsNotSet() {
            // Given
            factory.setAvailableStoreTransactionIds(List.of(10L));

            // When & Then
            assertThatThrownBy(() -> factory.generate(1))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(StoreSaleItemFactory.ERROR_PRODUCT_IDS_NOT_SET);
        }
    }

    @Nested
    @DisplayName("Round-robin assignment")
    class RoundRobinAssignment {

        @Test
        @DisplayName("Should assign transaction and product IDs in round-robin fashion")
        void shouldAssignIds_inRoundRobinFashion() {
            // Given
            factory.setAvailableStoreTransactionIds(List.of(100L, 200L));
            factory.setAvailableStoreProductIds(List.of(300L, 400L));

            // When
            List<StoreSaleItemRequest> result = factory.generate(4);

            // Then
            assertThat(result.get(0).storeTransactionId()).isEqualTo(100L);
            assertThat(result.get(0).storeProductId()).isEqualTo(300L);
            assertThat(result.get(1).storeTransactionId()).isEqualTo(200L);
            assertThat(result.get(1).storeProductId()).isEqualTo(400L);
            assertThat(result.get(2).storeTransactionId()).isEqualTo(100L);
            assertThat(result.get(2).storeProductId()).isEqualTo(300L);
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single sale item")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleSaleItem() {
            // Given
            factory.setAvailableStoreTransactionIds(List.of(10L));
            factory.setAvailableStoreProductIds(List.of(30L));

            // When
            List<StoreSaleItemRequest> result = factory.generate(1);
            StoreSaleItemRequest dto = result.get(0);

            // Then
            assertThat(dto.storeTransactionId()).isEqualTo(10L);
            assertThat(dto.storeProductId()).isEqualTo(30L);
            assertThat(dto.quantity()).isPositive();
            assertThat(dto.unitPriceAtSale()).isPositive();
            assertThat(dto.itemTotal()).isPositive();
        }

        @Test
        @DisplayName("Should calculate itemTotal as quantity times unitPriceAtSale")
        void shouldCalculateItemTotal_asQuantityTimesUnitPriceAtSale() {
            // Given
            factory.setAvailableStoreTransactionIds(List.of(10L));
            factory.setAvailableStoreProductIds(List.of(30L));

            // When
            List<StoreSaleItemRequest> result = factory.generate(1);
            StoreSaleItemRequest dto = result.get(0);

            // Then
            assertThat(dto.itemTotal()).isEqualByComparingTo(
                    dto.unitPriceAtSale().multiply(java.math.BigDecimal.valueOf(dto.quantity())));
        }
    }
}
