/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.notification;

import com.akademiaplus.util.mock.notification.EmailFactory.EmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EmailFactory}.
 */
@DisplayName("EmailFactory")
class EmailFactoryTest {

    private EmailFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EmailFactory(new EmailDataGenerator());
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
            List<EmailRequest> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given & When
            List<EmailRequest> result = factory.generate(0);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single email")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleEmail() {
            // Given & When
            List<EmailRequest> result = factory.generate(1);
            EmailRequest dto = result.get(0);

            // Then
            assertThat(dto.subject()).isNotBlank();
            assertThat(dto.body()).isNotBlank();
            assertThat(dto.sender()).isNotBlank();
        }
    }
}
