package com.akademiaplus.infra.persistence.idassigner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IdGenerationStrategy
 */
class IdGenerationStrategyTest {

    private IdGenerationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IdGenerationStrategy();
    }

    @Test
    void shouldGenerateIdWhenCurrentIdIsNull() {
        // Given
        Object currentId = null;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateIdWhenCurrentIdIsEmptyString() {
        // Given
        String currentId = "";

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateIdWhenCurrentIdIsZeroLong() {
        // Given
        Long currentId = 0L;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldGenerateIdWhenCurrentIdIsZeroInteger() {
        // Given
        Integer currentId = 0;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldNotGenerateIdWhenCurrentIdIsValidLong() {
        // Given
        Long currentId = 123L;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotGenerateIdWhenCurrentIdIsValidInteger() {
        // Given
        Integer currentId = 456;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotGenerateIdWhenCurrentIdIsValidString() {
        // Given
        String currentId = "abc-123";

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void shouldNotGenerateIdWhenCurrentIdIsNegativeNumber() {
        // Given
        Long currentId = -1L;

        // When
        boolean result = strategy.shouldGenerateId(currentId);

        // Then
        assertThat(result).isFalse();
    }
}