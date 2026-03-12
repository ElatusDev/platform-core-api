/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import com.akademiaplus.domain.TransformType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TransformExecutor Tests")
class TransformExecutorTest {

    private final TransformExecutor executor = new TransformExecutor();

    @Nested
    @DisplayName("NONE Transform")
    class NoneTransform {

        @Test
        @DisplayName("Should return value unchanged when given NONE transform")
        void shouldReturnValueUnchanged_whenGivenNoneTransform() {
            // Given
            String value = "Hello World";

            // When
            Map<String, String> result = executor.apply(value, TransformType.NONE, "field");

            // Then
            assertThat(result).containsEntry("field", "Hello World");
        }
    }

    @Nested
    @DisplayName("SPLIT_NAME Transform")
    class SplitNameTransform {

        @Test
        @DisplayName("Should split full name into first and last when given two-part name")
        void shouldSplitFullName_whenGivenTwoPartName() {
            // Given
            String value = "John Doe";

            // When
            Map<String, String> result = executor.apply(value, TransformType.SPLIT_NAME, "name");

            // Then
            assertThat(result).containsEntry("firstName", "John");
            assertThat(result).containsEntry("lastName", "Doe");
        }

        @Test
        @DisplayName("Should put everything in firstName when given single name")
        void shouldPutEverythingInFirstName_whenGivenSingleName() {
            // Given
            String value = "Madonna";

            // When
            Map<String, String> result = executor.apply(value, TransformType.SPLIT_NAME, "name");

            // Then
            assertThat(result).containsEntry("firstName", "Madonna");
            assertThat(result).containsEntry("lastName", "");
        }

        @Test
        @DisplayName("Should split on last space when given multi-part name")
        void shouldSplitOnLastSpace_whenGivenMultiPartName() {
            // Given
            String value = "Maria del Carmen Garcia";

            // When
            Map<String, String> result = executor.apply(value, TransformType.SPLIT_NAME, "name");

            // Then
            assertThat(result).containsEntry("firstName", "Maria del Carmen");
            assertThat(result).containsEntry("lastName", "Garcia");
        }
    }

    @Nested
    @DisplayName("NORMALIZE_PHONE Transform")
    class NormalizePhoneTransform {

        @Test
        @DisplayName("Should strip non-digit characters when given formatted phone")
        void shouldStripNonDigits_whenGivenFormattedPhone() {
            // Given
            String value = "+1 (555) 123-4567";

            // When
            Map<String, String> result = executor.apply(value, TransformType.NORMALIZE_PHONE, "phoneNumber");

            // Then
            assertThat(result).containsEntry("phoneNumber", "+15551234567");
        }
    }

    @Nested
    @DisplayName("DATE_FROM_AGE Transform")
    class DateFromAgeTransform {

        @Test
        @DisplayName("Should calculate approximate birth date when given age")
        void shouldCalculateBirthDate_whenGivenAge() {
            // Given
            String value = "25";

            // When
            Map<String, String> result = executor.apply(value, TransformType.DATE_FROM_AGE, "birthDate");

            // Then
            String expected = LocalDate.now().minusYears(25).toString();
            assertThat(result).containsEntry("birthDate", expected);
        }

        @Test
        @DisplayName("Should throw exception when given non-integer age")
        void shouldThrowException_whenGivenNonIntegerAge() {
            // Given
            String value = "twenty";

            // When & Then
            assertThatThrownBy(() -> executor.apply(value, TransformType.DATE_FROM_AGE, "birthDate"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Cannot convert");
        }
    }

    @Nested
    @DisplayName("Case Transforms")
    class CaseTransforms {

        @Test
        @DisplayName("Should uppercase value when given UPPERCASE transform")
        void shouldUppercase_whenGivenUppercaseTransform() {
            // Given
            String value = "hello";

            // When
            Map<String, String> result = executor.apply(value, TransformType.UPPERCASE, "field");

            // Then
            assertThat(result).containsEntry("field", "HELLO");
        }

        @Test
        @DisplayName("Should lowercase value when given LOWERCASE transform")
        void shouldLowercase_whenGivenLowercaseTransform() {
            // Given
            String value = "HELLO";

            // When
            Map<String, String> result = executor.apply(value, TransformType.LOWERCASE, "field");

            // Then
            assertThat(result).containsEntry("field", "hello");
        }

        @Test
        @DisplayName("Should trim whitespace when given TRIM transform")
        void shouldTrimWhitespace_whenGivenTrimTransform() {
            // Given
            String value = "  hello  ";

            // When
            Map<String, String> result = executor.apply(value, TransformType.TRIM, "field");

            // Then
            assertThat(result).containsEntry("field", "hello");
        }
    }

    @Nested
    @DisplayName("Null Handling")
    class NullHandling {

        @Test
        @DisplayName("Should treat null value as empty string")
        void shouldTreatNullAsEmptyString() {
            // Given / When
            Map<String, String> result = executor.apply(null, TransformType.NONE, "field");

            // Then
            assertThat(result).containsEntry("field", "");
        }
    }
}
