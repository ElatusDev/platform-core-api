package com.akademiaplus.util.mock.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersonDataGenerator")
class PersonDataGeneratorTest {

    private PersonDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PersonDataGenerator();
    }

    @Nested
    @DisplayName("Name generation")
    class NameGeneration {

        @Test
        void shouldGenerateNonBlankFirstName() {
            // Given
            // generator initialized in setUp

            // When
            String firstName = generator.firstName();

            // Then
            assertThat(firstName).isNotBlank();
        }

        @Test
        void shouldGenerateNonBlankLastName() {
            // Given
            // generator initialized in setUp

            // When
            String lastName = generator.lastName();

            // Then
            assertThat(lastName).isNotBlank();
        }
    }

    @Nested
    @DisplayName("Email generation")
    class EmailGeneration {

        @Test
        void shouldGenerateEmailWithAtSign_whenGivenFirstAndLastName() {
            // Given
            String firstName = "Carlos";
            String lastName = "García";

            // When
            String email = generator.email(firstName, lastName);

            // Then
            assertThat(email).contains("@");
        }

        @Test
        void shouldGenerateEmailStartingWithFirstInitialAndLastName() {
            // Given
            String firstName = "María";
            String lastName = "Lopez";

            // When
            String email = generator.email(firstName, lastName);

            // Then
            String localPart = email.substring(0, email.indexOf("@"));
            assertThat(localPart).startsWith("m");
            assertThat(localPart).containsIgnoringCase("lopez");
        }
    }

    @Nested
    @DisplayName("Username generation")
    class UsernameGeneration {

        @Test
        void shouldGenerateLowercaseAlphanumericUsername() {
            // Given
            String firstName = "Ana";
            String lastName = "Pérez";

            // When
            String username = generator.username(firstName, lastName);

            // Then
            assertThat(username).matches("[a-z0-9]+");
        }
    }

    @Nested
    @DisplayName("Password generation")
    class PasswordGeneration {

        @Test
        void shouldGeneratePasswordWithMixedCharacterTypes() {
            // Given
            // generator initialized in setUp

            // When
            String password = generator.password();

            // Then
            assertThat(password).hasSizeGreaterThanOrEqualTo(10);
            assertThat(password).matches(".*[A-Z].*");
            assertThat(password).matches(".*[a-z].*");
            assertThat(password).matches(".*[0-9].*");
        }
    }

    @Nested
    @DisplayName("Contact info generation")
    class ContactInfoGeneration {

        @Test
        void shouldGenerateNonBlankPhoneNumber() {
            // Given
            // generator initialized in setUp

            // When
            String phone = generator.phoneNumber();

            // Then
            assertThat(phone).isNotBlank();
        }

        @Test
        void shouldGenerateFiveDigitZipCode() {
            // Given
            // generator initialized in setUp

            // When
            String zipCode = generator.zipCode();

            // Then
            assertThat(zipCode).hasSize(5);
            assertThat(zipCode).matches("\\d{5}");
        }
    }

    @Nested
    @DisplayName("Date generation")
    class DateGeneration {

        @Test
        void shouldGenerateBirthdateWithinAgeRange_whenGivenMinAndMax() {
            // Given
            int minAge = 20;
            int maxAge = 30;

            // When
            LocalDate birthdate = generator.birthdate(minAge, maxAge);

            // Then
            LocalDate today = LocalDate.now();
            LocalDate oldestAllowed = today.minusYears(maxAge + 1);
            LocalDate youngestAllowed = today.minusYears(minAge).plusDays(1);
            assertThat(birthdate).isAfter(oldestAllowed);
            assertThat(birthdate).isBefore(youngestAllowed);
        }

        @Test
        void shouldGenerateEntryDateWithinLastFiveYears() {
            // Given
            // generator initialized in setUp

            // When
            LocalDate entryDate = generator.entryDate();

            // Then
            LocalDate today = LocalDate.now();
            LocalDate fiveYearsAgo = today.minusDays(1825);
            assertThat(entryDate).isAfterOrEqualTo(fiveYearsAgo);
            assertThat(entryDate).isBeforeOrEqualTo(today);
        }
    }

    @Nested
    @DisplayName("Profile picture generation")
    class ProfilePictureGeneration {

        @Test
        void shouldGeneratePresentProfilePicture() {
            // Given
            // generator initialized in setUp

            // When
            JsonNullable<byte[]> picture = generator.profilePicture();

            // Then
            assertThat(picture.isPresent()).isTrue();
            assertThat(picture.get()).isNotEmpty();
        }
    }
}
