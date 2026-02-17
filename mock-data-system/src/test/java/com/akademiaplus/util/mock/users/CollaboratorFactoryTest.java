package com.akademiaplus.util.mock.users;

import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollaboratorFactory")
class CollaboratorFactoryTest {

    private CollaboratorFactory factory;

    @BeforeEach
    void setUp() {
        PersonDataGenerator personData = new PersonDataGenerator();
        CollaboratorDataGenerator generator = new CollaboratorDataGenerator(personData);
        factory = new CollaboratorFactory(generator);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        void shouldGenerateExactCount_whenGivenPositiveNumber() {
            // Given
            int expectedCount = 5;

            // When
            List<CollaboratorCreationRequestDTO> result = factory.generate(expectedCount);

            // Then
            assertThat(result).hasSize(expectedCount);
        }

        @Test
        void shouldGenerateEmptyList_whenGivenZero() {
            // Given
            int zeroCount = 0;

            // When
            List<CollaboratorCreationRequestDTO> result = factory.generate(zeroCount);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("DTO population")
    class DtoPopulation {

        @Test
        void shouldPopulateAllRequiredFields_whenGeneratingSingleCollaborator() {
            // Given
            int singleRecord = 1;

            // When
            List<CollaboratorCreationRequestDTO> result = factory.generate(singleRecord);
            CollaboratorCreationRequestDTO dto = result.get(0);

            // Then
            assertThat(dto.getSkills()).isNotBlank();
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
