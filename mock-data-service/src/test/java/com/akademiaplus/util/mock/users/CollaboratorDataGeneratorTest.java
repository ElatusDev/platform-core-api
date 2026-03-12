package com.akademiaplus.util.mock.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollaboratorDataGenerator")
class CollaboratorDataGeneratorTest {

    private CollaboratorDataGenerator generator;

    private static final List<String> VALID_SKILL_SETS = List.of(
            "Mathematics, Physics",
            "Chemistry, Biology",
            "Spanish Literature, Grammar",
            "English, French",
            "Music, Art",
            "Physical Education, Dance",
            "Programming, Robotics",
            "History, Social Studies",
            "Accounting, Finance",
            "Yoga, Meditation"
    );

    private static final List<String> VALID_ROLES = List.of(
            "COLLABORATOR", "INSTRUCTOR", "SPECIALIST"
    );

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        generator = new CollaboratorDataGenerator(personData);
    }

    @Nested
    @DisplayName("Collaborator-specific field generation")
    class CollaboratorSpecificFields {

        @Test
        void shouldReturnValidSkills() {
            // Given
            // generator initialized in setUp

            // When
            String skills = generator.skills();

            // Then
            assertThat(skills).isIn(VALID_SKILL_SETS);
        }

        @Test
        void shouldReturnValidRole() {
            // Given
            // generator initialized in setUp

            // When
            String role = generator.role();

            // Then
            assertThat(role).isIn(VALID_ROLES);
        }

        @Test
        void shouldReturnBirthdateBetweenAge25And60() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate birthdate = generator.birthdate();

            // Then
            LocalDate oldestAllowed = today.minusYears(61);
            LocalDate youngestAllowed = today.minusYears(25).plusDays(1);
            assertThat(birthdate).isAfter(oldestAllowed);
            assertThat(birthdate).isBefore(youngestAllowed);
        }
    }
}
