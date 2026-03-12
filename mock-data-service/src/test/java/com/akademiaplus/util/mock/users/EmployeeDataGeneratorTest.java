package com.akademiaplus.util.mock.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmployeeDataGenerator")
class EmployeeDataGeneratorTest {

    private EmployeeDataGenerator generator;

    private static final List<String> VALID_EMPLOYEE_TYPES = List.of(
            "INSTRUCTOR", "ADMINISTRATOR", "COORDINATOR", "MANAGER", "ASSISTANT"
    );

    private static final List<String> VALID_ROLES = List.of(
            "EMPLOYEE", "ADMIN", "SUPERVISOR", "MANAGER", "USER"
    );

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        generator = new EmployeeDataGenerator(personData);
    }

    @Nested
    @DisplayName("Employee-specific field generation")
    class EmployeeSpecificFields {

        @Test
        void shouldReturnValidEmployeeType() {
            // Given
            // generator initialized in setUp

            // When
            String type = generator.employeeType();

            // Then
            assertThat(type).isIn(VALID_EMPLOYEE_TYPES);
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
        void shouldReturnBirthdateBetweenAge22And65() {
            // Given
            LocalDate today = LocalDate.now();

            // When
            LocalDate birthdate = generator.birthdate();

            // Then
            LocalDate oldestAllowed = today.minusYears(66);
            LocalDate youngestAllowed = today.minusYears(22).plusDays(1);
            assertThat(birthdate).isAfter(oldestAllowed);
            assertThat(birthdate).isBefore(youngestAllowed);
        }
    }
}
