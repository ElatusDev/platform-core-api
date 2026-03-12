package com.akademiaplus.util.mock.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdultStudentDataGenerator")
class AdultStudentDataGeneratorTest {

    private AdultStudentDataGenerator generator;

    private static final List<String> VALID_PROVIDERS = List.of(
            "GOOGLE", "FACEBOOK", "APPLE"
    );

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        generator = new AdultStudentDataGenerator(personData);
    }

    @Nested
    @DisplayName("AdultStudent-specific field generation")
    class AdultStudentSpecificFields {

        @Test
        void shouldReturnValidProvider() {
            // Given
            // generator initialized in setUp

            // When
            String provider = generator.provider();

            // Then
            assertThat(provider).isIn(VALID_PROVIDERS);
        }

        @Test
        void shouldReturnTokenWithOauthPrefix() {
            // Given
            // generator initialized in setUp

            // When
            String token = generator.token();

            // Then
            assertThat(token).startsWith("oauth_");
            assertThat(token).hasSize(38); // "oauth_" (6) + 32 hex chars
        }

        @Test
        void shouldReturnBirthdateBetweenAge18And55() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate birthdate = generator.birthdate();

            // Then
            LocalDate oldestAllowed = today.minusYears(56);
            LocalDate youngestAllowed = today.minusYears(18).plusDays(1);
            assertThat(birthdate).isAfter(oldestAllowed);
            assertThat(birthdate).isBefore(youngestAllowed);
        }
    }
}
