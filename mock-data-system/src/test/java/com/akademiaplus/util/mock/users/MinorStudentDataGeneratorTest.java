package com.akademiaplus.util.mock.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MinorStudentDataGenerator")
class MinorStudentDataGeneratorTest {

    private MinorStudentDataGenerator generator;

    private static final List<String> VALID_PROVIDERS = List.of(
            "GOOGLE", "FACEBOOK", "APPLE"
    );

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        generator = new MinorStudentDataGenerator(personData);
    }

    @Nested
    @DisplayName("MinorStudent-specific field generation")
    class MinorStudentSpecificFields {

        @Test
        @DisplayName("Should return valid provider when called")
        void shouldReturnValidProvider_whenCalled() {
            // Given
            // generator initialized in setUp

            // When
            String provider = generator.provider();

            // Then
            assertThat(provider).isIn(VALID_PROVIDERS);
        }

        @Test
        @DisplayName("Should return token with oauth prefix when called")
        void shouldReturnTokenWithOauthPrefix_whenCalled() {
            // Given
            // generator initialized in setUp

            // When
            String token = generator.token();

            // Then
            assertThat(token).startsWith("oauth_");
            assertThat(token).hasSize(38); // "oauth_" (6) + 32 hex chars
        }

        @Test
        @DisplayName("Should return birthdate between age 6 and 17 when called")
        void shouldReturnBirthdateBetweenAge6And17_whenCalled() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate birthdate = generator.birthdate();

            // Then
            LocalDate oldestAllowed = today.minusYears(18);
            LocalDate youngestAllowed = today.minusYears(6).plusDays(1);
            assertThat(birthdate).isAfter(oldestAllowed);
            assertThat(birthdate).isBefore(youngestAllowed);
        }
    }
}
