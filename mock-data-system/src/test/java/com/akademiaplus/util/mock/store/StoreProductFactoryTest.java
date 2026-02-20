/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.store;

import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StoreProductFactory}.
 */
@DisplayName("StoreProductFactory")
class StoreProductFactoryTest {

    private StoreProductFactory factory;

    @BeforeEach
    void setUp() {
        StoreProductDataGenerator generator = new StoreProductDataGenerator();
        factory = new StoreProductFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 5;

            // When
            List<StoreProductCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<StoreProductCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single product")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleProduct() {
            // Given
            int singleRecord = 1;

            // When
            List<StoreProductCreationRequestDTO> result = factory.generate(singleRecord);
            StoreProductCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getName()).isNotBlank();
            assertThat(dto.getDescription()).isNotBlank();
            assertThat(dto.getPrice()).isPositive();
            assertThat(dto.getStockQuantity()).isPositive();
        }
    }
}
