package com.akademiaplus.util.mock.users;

import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmployeeFactory")
class EmployeeFactoryTest {

    private EmployeeFactory factory;

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        EmployeeDataGenerator generator = new EmployeeDataGenerator(personData);
        factory = new EmployeeFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 7;

            // When
            List<EmployeeCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<EmployeeCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        void shouldPopulateAllRequiredFields_whenGeneratingSingleEmployee() {
            // Given
            int singleRecord = 1;

            // When
            List<EmployeeCreationRequestDTO> result = factory.generate(singleRecord);
            EmployeeCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getEmployeeType()).isNotBlank();
            assertThat(dto.getBirthdate()).isNotNull();
            assertThat(dto.getEntryDate()).isNotNull();
            assertThat(dto.getFirstName()).isNotBlank();
            assertThat(dto.getLastName()).isNotBlank();
            assertThat(dto.getEmail()).contains("@");
            assertThat(dto.getPhoneNumber()).isNotBlank();
            assertThat(dto.getAddress()).isNotBlank();
            assertThat(dto.getZipCode()).hasSize(5);
            assertThat(dto.getUsername()).isNotBlank();
            assertThat(dto.getPassword()).isNotBlank();
            assertThat(dto.getRole()).isNotBlank();
            assertThat(dto.getProfilePicture().isPresent()).isTrue();
        }
    }
}
