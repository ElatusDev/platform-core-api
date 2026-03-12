package com.akademiaplus.util.mock.users;

import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AdultStudentFactory")
class AdultStudentFactoryTest {

    private AdultStudentFactory factory;

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        AdultStudentDataGenerator generator = new AdultStudentDataGenerator(personData);
        factory = new AdultStudentFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 3;

            // When
            List<AdultStudentCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<AdultStudentCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        void shouldPopulateAllRequiredFields_whenGeneratingSingleAdultStudent() {
            // Given
            int singleRecord = 1;

            // When
            List<AdultStudentCreationRequestDTO> result = factory.generate(singleRecord);
            AdultStudentCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getBirthdate()).isNotNull();
            assertThat(dto.getFirstName()).isNotBlank();
            assertThat(dto.getLastName()).isNotBlank();
            assertThat(dto.getEmail()).contains("@");
            assertThat(dto.getPhoneNumber()).isNotBlank();
            assertThat(dto.getAddress()).isNotBlank();
            assertThat(dto.getZipCode()).hasSize(5);
            assertThat(dto.getProvider()).isNotBlank();
            assertThat(dto.getToken()).startsWith("oauth_");
            assertThat(dto.getProfilePicture().isPresent()).isTrue();
        }
    }
}
