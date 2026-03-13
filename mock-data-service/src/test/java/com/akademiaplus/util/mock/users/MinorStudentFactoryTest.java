package com.akademiaplus.util.mock.users;

import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MinorStudentFactory")
class MinorStudentFactoryTest {

    private MinorStudentFactory factory;

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        MinorStudentDataGenerator generator = new MinorStudentDataGenerator(personData);
        factory = new MinorStudentFactory(generator);
    }

    @Nested
    @DisplayName("Tutor ID validation")
    class TutorIdValidation {

        @Test
        @DisplayName("Should throw IllegalStateException when available tutor IDs is empty")
        void shouldThrowIllegalStateException_whenAvailableTutorIdsIsEmpty() {
            // Given
            // factory has no tutor IDs set (default empty list)
            int count = 1;

            // When & Then
            assertThatThrownBy(() -> factory.generate(count))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(MinorStudentFactory.ERROR_TUTOR_IDS_NOT_SET);
        }

        @Test
        @DisplayName("Should assign tutor IDs in round-robin when generating multiple students")
        void shouldAssignTutorIdsInRoundRobin_whenGeneratingMultipleStudents() {
            // Given
            factory.setAvailableTutorIds(List.of(100L, 200L));
            int count = 4;

            // When
            List<MinorStudentCreationRequestDTO> result = factory.generate(count);

            // Then
            assertThat(result.get(0).getTutorId()).isEqualTo(100L);
            assertThat(result.get(1).getTutorId()).isEqualTo(200L);
            assertThat(result.get(2).getTutorId()).isEqualTo(100L);
            assertThat(result.get(3).getTutorId()).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should generate exact count when given positive number")
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            factory.setAvailableTutorIds(List.of(1L));
            int expectedCount = 3;

            // When
            List<MinorStudentCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        @DisplayName("Should generate empty list when given zero")
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            factory.setAvailableTutorIds(List.of(1L));
            int zeroCount = 0;

            // When
            List<MinorStudentCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        @DisplayName("Should populate all required fields when generating single minor student")
        void shouldPopulateAllRequiredFields_whenGeneratingSingleMinorStudent() {
            // Given
            Long tutorId = 42L;
            factory.setAvailableTutorIds(List.of(tutorId));
            int singleRecord = 1;

            // When
            List<MinorStudentCreationRequestDTO> result = factory.generate(singleRecord);
            MinorStudentCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getBirthdate()).isNotNull();
            assertThat(dto.getTutorId()).isEqualTo(tutorId);
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
