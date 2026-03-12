package com.akademiaplus.util.mock.users;

import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TutorFactory")
class TutorFactoryTest {

    private TutorFactory factory;

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        TutorDataGenerator generator = new TutorDataGenerator(personData);
        factory = new TutorFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 3;

            // When
            List<TutorCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<TutorCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single tutor")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleTutor() {
            // Given
            int singleRecord = 1;

            // When
            List<TutorCreationRequestDTO> result = factory.generate(singleRecord);
            TutorCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getBirthdate()).isNotNull();
            assertThat(dto.getFirstName()).isNotBlank();
            assertThat(dto.getLastName()).isNotBlank();
            assertThat(dto.getEmail()).contains("@");
            assertThat(dto.getPhoneNumber()).isNotBlank();
            assertThat(dto.getAddress()).isNotBlank();
            assertThat(dto.getZipCode()).hasSize(5);
            assertThat(dto.getProvider().isPresent()).isTrue();
            assertThat(dto.getToken().isPresent()).isTrue();
        }
    }
}
