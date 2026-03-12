package com.akademiaplus.infra.persistence.idassigner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IdGenerationStrategy
 */
@DisplayName("IdGenerationStrategy Tests")
class IdGenerationStrategyTest {

    private IdGenerationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IdGenerationStrategy();
    }

    @Nested
    @DisplayName("shouldGenerateId returns true")
    class ShouldGenerateId {

        @Test
        @DisplayName("Should generate ID when current ID is null")
        void shouldGenerateId_whenCurrentIdIsNull() {
            // Given
            Object currentId = null;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should generate ID when current ID is empty string")
        void shouldGenerateId_whenCurrentIdIsEmptyString() {
            // Given
            String currentId = "";

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should generate ID when current ID is zero Long")
        void shouldGenerateId_whenCurrentIdIsZeroLong() {
            // Given
            Long currentId = 0L;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should generate ID when current ID is zero Integer")
        void shouldGenerateId_whenCurrentIdIsZeroInteger() {
            // Given
            Integer currentId = 0;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("shouldGenerateId returns false")
    class ShouldNotGenerateId {

        @Test
        @DisplayName("Should not generate ID when current ID is valid Long")
        void shouldNotGenerateId_whenCurrentIdIsValidLong() {
            // Given
            Long currentId = 123L;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not generate ID when current ID is valid Integer")
        void shouldNotGenerateId_whenCurrentIdIsValidInteger() {
            // Given
            Integer currentId = 456;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not generate ID when current ID is valid String")
        void shouldNotGenerateId_whenCurrentIdIsValidString() {
            // Given
            String currentId = "abc-123";

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not generate ID when current ID is negative number")
        void shouldNotGenerateId_whenCurrentIdIsNegativeNumber() {
            // Given
            Long currentId = -1L;

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should not generate ID when current ID is non-numeric non-string object")
        void shouldNotGenerateId_whenCurrentIdIsNonNumericObject() {
            // Given
            Object currentId = new Object();

            // When
            boolean result = strategy.shouldGenerateId(currentId);

            // Then
            assertThat(result).isFalse();
        }
    }
}
