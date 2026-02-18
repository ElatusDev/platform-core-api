/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.mapping;

import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.config.ModelMapperConfig;
import openapi.akademiaplus.domain.user.management.dto.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that {@link ModelMapper} (configured via {@link ModelMapperConfig})
 * correctly converts every OpenAPI creation-request DTO into its target
 * JPA data model.
 * <p>
 * Uses a <strong>real</strong> {@code ModelMapper} — not mocked — to surface
 * field-name mismatches, type mismatches, and unwanted deep-matching side effects
 * that unit tests with mocked mappers cannot detect.
 * <p>
 * Each nested class covers one conversion pair. Fields that are set manually by
 * the use case (e.g. {@code entryDate}, {@code emailHash}, {@code customerAuth})
 * are explicitly verified as {@code null} here to document the boundary between
 * ModelMapper's responsibility and the use case's.
 */
@DisplayName("ModelMapper DTO → DataModel Conversion")
class ModelMapperConversionTest {

    private static ModelMapper modelMapper;

    // ── Shared test constants ────────────────────────────────────────────
    public static final String FIRST_NAME = "María";
    public static final String LAST_NAME = "González";
    public static final String EMAIL = "maria.gonzalez@example.com";
    public static final String PHONE_NUMBER = "+525551234567";
    public static final String ADDRESS = "Av. Reforma 222, CDMX";
    public static final String ZIP_CODE = "06600";
    public static final LocalDate BIRTHDATE = LocalDate.of(1990, 3, 15);
    public static final LocalDate ENTRY_DATE = LocalDate.of(2025, 1, 10);
    public static final String PROVIDER = "GOOGLE";
    public static final String TOKEN = "oauth_token_abc123";
    public static final String USERNAME = "mgonzalez";
    public static final String PASSWORD = "s3cure!Pass";
    public static final String ROLE = "INSTRUCTOR";
    public static final String EMPLOYEE_TYPE = "FULL_TIME";
    public static final String SKILLS = "Java, Spring Boot";
    public static final byte[] PROFILE_PICTURE = new byte[]{1, 2, 3, 4};

    /**
     * Named TypeMap constant — must match {@code TutorCreationUseCase.TUTOR_MAP_NAME}.
     */
    public static final String TUTOR_MAP_NAME = "tutorMap";

    /**
     * Named TypeMap constant — must match {@code TutorCreationUseCase.MINOR_STUDENT_MAP_NAME}.
     * Required to prevent deep-matching pollution of {@code tutorId} into
     * {@code minorStudentId} and {@code tutor.tutorId}.
     */
    public static final String MINOR_STUDENT_MAP_NAME = "minorStudentMap";

    @BeforeAll
    static void setUpMapper() {
        modelMapper = new ModelMapperConfig().modelMapper();

        // Disable implicit mapping before creating TypeMaps to prevent
        // ModelMapper from eagerly matching DTO fields into nested paths
        // before skip rules are registered.
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        modelMapper.createTypeMap(
                TutorCreationRequestDTO.class,
                TutorDataModel.class,
                TUTOR_MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(TutorDataModel::setTutorId);
            mapper.skip(TutorDataModel::setPersonPII);
            mapper.skip(TutorDataModel::setCustomerAuth);
        }).implicitMappings();

        modelMapper.createTypeMap(
                MinorStudentCreationRequestDTO.class,
                MinorStudentDataModel.class,
                MINOR_STUDENT_MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MinorStudentDataModel::setMinorStudentId);
            mapper.skip(MinorStudentDataModel::setTutor);
            mapper.skip(MinorStudentDataModel::setCustomerAuth);
            mapper.skip(MinorStudentDataModel::setPersonPII);
        }).implicitMappings();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PersonPII mapping — shared across ALL people entities
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TutorCreationRequestDTO → PersonPIIDataModel")
    class TutorDtoToPersonPii {

        @Test
        @DisplayName("Should map firstName when names match exactly")
        void shouldMapFirstName_whenNamesMatchExactly() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
        }

        @Test
        @DisplayName("Should map lastName when names match exactly")
        void shouldMapLastName_whenNamesMatchExactly() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
        }

        @Test
        @DisplayName("Should map email when names match exactly")
        void shouldMapEmail_whenNamesMatchExactly() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getEmail()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("Should map address when names match exactly")
        void shouldMapAddress_whenNamesMatchExactly() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getAddress()).isEqualTo(ADDRESS);
        }

        @Test
        @DisplayName("Should map zipCode when names match exactly")
        void shouldMapZipCode_whenNamesMatchExactly() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getZipCode()).isEqualTo(ZIP_CODE);
        }

        @Test
        @DisplayName("Should map phoneNumber to phone when token names differ")
        void shouldMapPhoneNumber_whenTokenNamesDiffer() {
            // Given — DTO has 'phoneNumber', PersonPII has 'phone'
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then — verifies ModelMapper resolves the phoneNumber→phone mismatch
            assertThat(result.getPhone())
                    .as("DTO 'phoneNumber' must map to PersonPII 'phone'")
                    .isEqualTo(PHONE_NUMBER);
        }

        @Test
        @DisplayName("Should leave entity ID null when DTO has no ID field")
        void shouldLeaveEntityIdNull_whenDtoHasNoIdField() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then — IDs are generated at app/db level, never from DTO
            assertThat(result.getPersonPiiId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }

        @Test
        @DisplayName("Should leave hash fields null when DTO carries no hashes")
        void shouldLeaveHashFieldsNull_whenDtoCarriesNoHashes() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then — hashes are computed by the use case, not by ModelMapper
            assertThat(result.getEmailHash()).isNull();
            assertThat(result.getPhoneHash()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TutorCreationRequestDTO → TutorDataModel
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TutorCreationRequestDTO → TutorDataModel")
    class TutorDtoToTutorModel {

        @Test
        @DisplayName("Should map birthdate to birthDate when camelCase tokens differ")
        void shouldMapBirthdate_whenCamelCaseTokensDiffer() {
            // Given — DTO has 'birthdate' (1 token), model has 'birthDate' (2 tokens)
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            TutorDataModel result = modelMapper.map(dto, TutorDataModel.class);

            // Then — verifies ModelMapper resolves the birthdate→birthDate mismatch
            assertThat(result.getBirthDate())
                    .as("DTO 'birthdate' must map to AbstractUser 'birthDate'")
                    .isEqualTo(BIRTHDATE);
        }

        @Test
        @DisplayName("Should leave tutorId null when creation DTO has no entity ID")
        void shouldLeaveTutorIdNull_whenCreationDtoHasNoEntityId() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            TutorDataModel result = modelMapper.map(dto, TutorDataModel.class);

            // Then — entity IDs are generated at app/db level
            assertThat(result.getTutorId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }

        @Test
        @DisplayName("Should leave entryDate null when use case sets it manually")
        void shouldLeaveEntryDateNull_whenUseCaseSetsItManually() {
            // Given
            TutorCreationRequestDTO dto = buildTutorDto();

            // When
            TutorDataModel result = modelMapper.map(dto, TutorDataModel.class);

            // Then — entryDate is set to LocalDate.now() by the use case
            assertThat(result.getEntryDate()).isNull();
        }

        @Test
        @DisplayName("Should not create CustomerAuth via deep matching when provider is JsonNullable")
        void shouldNotCreateCustomerAuth_whenProviderIsJsonNullable() {
            // Given — provider/token are JsonNullable<String>, not plain String
            TutorCreationRequestDTO dto = buildTutorDto();
            dto.setProvider(JsonNullable.of(PROVIDER));
            dto.setToken(JsonNullable.of(TOKEN));

            // When
            TutorDataModel result = modelMapper.map(dto, TutorDataModel.class);

            // Then — CustomerAuth is built manually by the use case
            assertThat(result.getCustomerAuth()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MinorStudentCreationRequestDTO → MinorStudentDataModel
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MinorStudentCreationRequestDTO → MinorStudentDataModel")
    class MinorStudentDtoToModel {

        private static final Long TUTOR_ID = 42L;

        @Test
        @DisplayName("Should map birthdate to birthDate when camelCase tokens differ")
        void shouldMapBirthdate_whenCamelCaseTokensDiffer() {
            // Given
            MinorStudentCreationRequestDTO dto = buildMinorStudentDto();

            // When
            MinorStudentDataModel result = modelMapper.map(dto, MinorStudentDataModel.class, MINOR_STUDENT_MAP_NAME);

            // Then
            assertThat(result.getBirthDate())
                    .as("DTO 'birthdate' must map to AbstractUser 'birthDate'")
                    .isEqualTo(BIRTHDATE);
        }

        @Test
        @DisplayName("Should leave minorStudentId null when creation DTO has no entity ID")
        void shouldLeaveMinorStudentIdNull_whenCreationDtoHasNoEntityId() {
            // Given
            MinorStudentCreationRequestDTO dto = buildMinorStudentDto();

            // When
            MinorStudentDataModel result = modelMapper.map(dto, MinorStudentDataModel.class, MINOR_STUDENT_MAP_NAME);

            // Then
            assertThat(result.getMinorStudentId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }

        @Test
        @DisplayName("Should not pollute tutor via deep matching of tutorId")
        void shouldNotPolluteTutor_whenDtoHasTutorId() {
            // Given — DTO has tutorId (Long), model has tutor (TutorDataModel)
            MinorStudentCreationRequestDTO dto = buildMinorStudentDto();

            // When
            MinorStudentDataModel result = modelMapper.map(dto, MinorStudentDataModel.class, MINOR_STUDENT_MAP_NAME);

            // Then — tutor is looked up by the use case, not created by ModelMapper
            assertThat(result.getTutor()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EmployeeCreationRequestDTO → EmployeeDataModel + InternalAuth
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EmployeeCreationRequestDTO → EmployeeDataModel")
    class EmployeeDtoToModel {

        @Test
        @DisplayName("Should map employeeType when names match exactly")
        void shouldMapEmployeeType_whenNamesMatchExactly() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            EmployeeDataModel result = modelMapper.map(dto, EmployeeDataModel.class);

            // Then
            assertThat(result.getEmployeeType()).isEqualTo(EMPLOYEE_TYPE);
        }

        @Test
        @DisplayName("Should map birthdate to birthDate when camelCase tokens differ")
        void shouldMapBirthdate_whenCamelCaseTokensDiffer() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            EmployeeDataModel result = modelMapper.map(dto, EmployeeDataModel.class);

            // Then
            assertThat(result.getBirthDate())
                    .as("DTO 'birthdate' must map to AbstractUser 'birthDate'")
                    .isEqualTo(BIRTHDATE);
        }

        @Test
        @DisplayName("Should map entryDate when names match exactly")
        void shouldMapEntryDate_whenNamesMatchExactly() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            EmployeeDataModel result = modelMapper.map(dto, EmployeeDataModel.class);

            // Then
            assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE);
        }

        @Test
        @DisplayName("Should leave employeeId null when creation DTO has no entity ID")
        void shouldLeaveEmployeeIdNull_whenCreationDtoHasNoEntityId() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            EmployeeDataModel result = modelMapper.map(dto, EmployeeDataModel.class);

            // Then
            assertThat(result.getEmployeeId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("EmployeeCreationRequestDTO → InternalAuthDataModel")
    class EmployeeDtoToInternalAuth {

        @Test
        @DisplayName("Should map username when names match exactly")
        void shouldMapUsername_whenNamesMatchExactly() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            InternalAuthDataModel result = modelMapper.map(dto, InternalAuthDataModel.class);

            // Then
            assertThat(result.getUsername()).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("Should map password when names match exactly")
        void shouldMapPassword_whenNamesMatchExactly() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            InternalAuthDataModel result = modelMapper.map(dto, InternalAuthDataModel.class);

            // Then
            assertThat(result.getPassword()).isEqualTo(PASSWORD);
        }

        @Test
        @DisplayName("Should map role when names match exactly")
        void shouldMapRole_whenNamesMatchExactly() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            InternalAuthDataModel result = modelMapper.map(dto, InternalAuthDataModel.class);

            // Then
            assertThat(result.getRole()).isEqualTo(ROLE);
        }

        @Test
        @DisplayName("Should leave internalAuthId null when DTO has no ID field")
        void shouldLeaveInternalAuthIdNull_whenDtoHasNoIdField() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            InternalAuthDataModel result = modelMapper.map(dto, InternalAuthDataModel.class);

            // Then
            assertThat(result.getInternalAuthId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }

        @Test
        @DisplayName("Should leave usernameHash null when DTO carries no hash")
        void shouldLeaveUsernameHashNull_whenDtoCarriesNoHash() {
            // Given
            EmployeeCreationRequestDTO dto = buildEmployeeDto();

            // When
            InternalAuthDataModel result = modelMapper.map(dto, InternalAuthDataModel.class);

            // Then
            assertThat(result.getUsernameHash()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CollaboratorCreationRequestDTO → CollaboratorDataModel + InternalAuth
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CollaboratorCreationRequestDTO → CollaboratorDataModel")
    class CollaboratorDtoToModel {

        @Test
        @DisplayName("Should map skills when names match exactly")
        void shouldMapSkills_whenNamesMatchExactly() {
            // Given
            CollaboratorCreationRequestDTO dto = buildCollaboratorDto();

            // When
            CollaboratorDataModel result = modelMapper.map(dto, CollaboratorDataModel.class);

            // Then
            assertThat(result.getSkills()).isEqualTo(SKILLS);
        }

        @Test
        @DisplayName("Should map birthdate to birthDate when camelCase tokens differ")
        void shouldMapBirthdate_whenCamelCaseTokensDiffer() {
            // Given
            CollaboratorCreationRequestDTO dto = buildCollaboratorDto();

            // When
            CollaboratorDataModel result = modelMapper.map(dto, CollaboratorDataModel.class);

            // Then
            assertThat(result.getBirthDate())
                    .as("DTO 'birthdate' must map to AbstractUser 'birthDate'")
                    .isEqualTo(BIRTHDATE);
        }

        @Test
        @DisplayName("Should map entryDate when names match exactly")
        void shouldMapEntryDate_whenNamesMatchExactly() {
            // Given
            CollaboratorCreationRequestDTO dto = buildCollaboratorDto();

            // When
            CollaboratorDataModel result = modelMapper.map(dto, CollaboratorDataModel.class);

            // Then
            assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE);
        }

        @Test
        @DisplayName("Should leave collaboratorId null when creation DTO has no entity ID")
        void shouldLeaveCollaboratorIdNull_whenCreationDtoHasNoEntityId() {
            // Given
            CollaboratorCreationRequestDTO dto = buildCollaboratorDto();

            // When
            CollaboratorDataModel result = modelMapper.map(dto, CollaboratorDataModel.class);

            // Then
            assertThat(result.getCollaboratorId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AdultStudentCreationRequestDTO → AdultStudentDataModel
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AdultStudentCreationRequestDTO → AdultStudentDataModel")
    class AdultStudentDtoToModel {

        @Test
        @DisplayName("Should map birthdate to birthDate when camelCase tokens differ")
        void shouldMapBirthdate_whenCamelCaseTokensDiffer() {
            // Given
            AdultStudentCreationRequestDTO dto = buildAdultStudentDto();

            // When
            AdultStudentDataModel result = modelMapper.map(dto, AdultStudentDataModel.class);

            // Then
            assertThat(result.getBirthDate())
                    .as("DTO 'birthdate' must map to AbstractUser 'birthDate'")
                    .isEqualTo(BIRTHDATE);
        }

        @Test
        @DisplayName("Should leave adultStudentId null when creation DTO has no entity ID")
        void shouldLeaveAdultStudentIdNull_whenCreationDtoHasNoEntityId() {
            // Given
            AdultStudentCreationRequestDTO dto = buildAdultStudentDto();

            // When
            AdultStudentDataModel result = modelMapper.map(dto, AdultStudentDataModel.class);

            // Then
            assertThat(result.getAdultStudentId()).isNull();
            assertThat(result.getTenantId()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AdultStudentCreationRequestDTO → PersonPIIDataModel (phone mismatch)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AdultStudentCreationRequestDTO → PersonPIIDataModel")
    class AdultStudentDtoToPersonPii {

        @Test
        @DisplayName("Should map phoneNumber to phone when token names differ")
        void shouldMapPhoneNumber_whenTokenNamesDiffer() {
            // Given
            AdultStudentCreationRequestDTO dto = buildAdultStudentDto();

            // When
            PersonPIIDataModel result = modelMapper.map(dto, PersonPIIDataModel.class);

            // Then
            assertThat(result.getPhone())
                    .as("DTO 'phoneNumber' must map to PersonPII 'phone'")
                    .isEqualTo(PHONE_NUMBER);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Builder helpers
    // ═══════════════════════════════════════════════════════════════════════

    private static TutorCreationRequestDTO buildTutorDto() {
        return new TutorCreationRequestDTO(
                BIRTHDATE, FIRST_NAME, LAST_NAME,
                EMAIL, PHONE_NUMBER, ADDRESS, ZIP_CODE
        );
    }

    private static MinorStudentCreationRequestDTO buildMinorStudentDto() {
        return new MinorStudentCreationRequestDTO(
                BIRTHDATE, 42L,
                FIRST_NAME, LAST_NAME,
                EMAIL, PHONE_NUMBER, ADDRESS, ZIP_CODE,
                PROVIDER, TOKEN
        );
    }

    private static AdultStudentCreationRequestDTO buildAdultStudentDto() {
        return new AdultStudentCreationRequestDTO(
                BIRTHDATE, FIRST_NAME, LAST_NAME,
                EMAIL, PHONE_NUMBER, ADDRESS, ZIP_CODE,
                PROVIDER, TOKEN
        );
    }

    private static EmployeeCreationRequestDTO buildEmployeeDto() {
        return new EmployeeCreationRequestDTO(
                EMPLOYEE_TYPE, BIRTHDATE, ENTRY_DATE,
                FIRST_NAME, LAST_NAME,
                EMAIL, PHONE_NUMBER, ADDRESS, ZIP_CODE,
                USERNAME, PASSWORD, ROLE
        );
    }

    private static CollaboratorCreationRequestDTO buildCollaboratorDto() {
        return new CollaboratorCreationRequestDTO(
                SKILLS, BIRTHDATE, ENTRY_DATE,
                FIRST_NAME, LAST_NAME,
                EMAIL, PHONE_NUMBER, ADDRESS, ZIP_CODE,
                USERNAME, PASSWORD, ROLE
        );
    }
}
