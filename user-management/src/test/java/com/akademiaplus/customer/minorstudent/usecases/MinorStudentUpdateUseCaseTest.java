/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("MinorStudentUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class MinorStudentUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long MINOR_STUDENT_ID = 42L;
    private static final Long PERSON_PII_ID = 99L;
    private static final Long TUTOR_ID = 10L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";

    @Mock private MinorStudentRepository minorStudentRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private MinorStudentUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MinorStudentUpdateUseCase(
                minorStudentRepository,
                tutorRepository,
                personPIIRepository,
                tenantContextHolder,
                modelMapper,
                piiNormalizer,
                hashingService
        );
    }

    private MinorStudentUpdateRequestDTO buildDto() {
        MinorStudentUpdateRequestDTO dto = new MinorStudentUpdateRequestDTO();
        dto.setBirthdate(LocalDate.of(2012, 5, 20));
        dto.setTutorId(TUTOR_ID);
        dto.setFirstName("Jane");
        dto.setLastName("Doe");
        dto.setEmail(TEST_EMAIL);
        dto.setPhoneNumber(TEST_PHONE);
        dto.setAddress("123 Main St");
        dto.setZipCode("12345");
        return dto;
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setPersonPiiId(PERSON_PII_ID);
        pii.setEmail(TEST_EMAIL);
        pii.setPhoneNumber(TEST_PHONE);
        return pii;
    }

    private MinorStudentDataModel buildExistingEntity(PersonPIIDataModel pii) {
        MinorStudentDataModel entity = new MinorStudentDataModel();
        entity.setPersonPII(pii);
        return entity;
    }

    private void stubPiiRehash(PersonPIIDataModel pii) {
        when(piiNormalizer.normalizeEmail(pii.getEmail())).thenReturn(NORMALIZED_EMAIL);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber())).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
    }

    private void stubFullSuccessPath(MinorStudentUpdateRequestDTO dto,
                                      MinorStudentDataModel existing,
                                      PersonPIIDataModel pii) {
        MinorStudentDataModel.MinorStudentCompositeId compositeId =
                new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
        TutorDataModel.TutorCompositeId tutorCompositeId =
                new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
        when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
        doNothing().when(modelMapper).map(dto, existing, MinorStudentUpdateUseCase.MAP_NAME);
        when(tutorRepository.findById(tutorCompositeId)).thenReturn(Optional.of(new TutorDataModel()));
        doNothing().when(modelMapper).map(dto, pii);
        stubPiiRehash(pii);
        when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                .thenReturn(false);
        when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                .thenReturn(false);
        when(minorStudentRepository.saveAndFlush(existing)).thenReturn(new MinorStudentDataModel());
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when minor student does not exist")
        void shouldThrowEntityNotFoundException_whenMinorStudentDoesNotExist() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(MINOR_STUDENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(MINOR_STUDENT_ID));
                    });
        }
    }

    @Nested
    @DisplayName("Tutor Validation")
    class TutorValidation {

        @Test
        @DisplayName("Should throw EntityNotFoundException when referenced tutor does not exist")
        void shouldThrowEntityNotFoundException_whenReferencedTutorDoesNotExist() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            MinorStudentDataModel existing = buildExistingEntity(pii);

            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            TutorDataModel.TutorCompositeId tutorCompositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, MinorStudentUpdateUseCase.MAP_NAME);
            when(tutorRepository.findById(tutorCompositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(MINOR_STUDENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                    });
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update entity and return response when minor student and tutor exist")
        void shouldUpdateEntityAndReturnResponse_whenMinorStudentAndTutorExist() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            MinorStudentDataModel existing = buildExistingEntity(pii);

            stubFullSuccessPath(dto, existing, pii);

            // When
            MinorStudentUpdateResponseDTO result = useCase.update(MINOR_STUDENT_ID, dto);

            // Then
            verify(minorStudentRepository).saveAndFlush(existing);
            assertThat(result.getMinorStudentId()).isEqualTo(MINOR_STUDENT_ID);
            assertThat(result.getMessage()).isEqualTo(MinorStudentUpdateUseCase.UPDATE_SUCCESS_MESSAGE);
        }
    }

    @Nested
    @DisplayName("PII Update")
    class PiiUpdate {

        @Test
        @DisplayName("Should rehash email and phone when PII fields are updated")
        void shouldRehashEmailAndPhone_whenPiiFieldsAreUpdated() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            MinorStudentDataModel existing = buildExistingEntity(pii);

            stubFullSuccessPath(dto, existing, pii);

            // When
            useCase.update(MINOR_STUDENT_ID, dto);

            // Then
            verify(piiNormalizer).normalizeEmail(TEST_EMAIL);
            verify(piiNormalizer).normalizePhoneNumber(TEST_PHONE);
            verify(hashingService).generateHash(NORMALIZED_EMAIL);
            verify(hashingService).generateHash(NORMALIZED_PHONE);
        }
    }

    @Nested
    @DisplayName("Duplicate Validation")
    class DuplicateValidation {

        @Test
        @DisplayName("Should throw DuplicateEntityException when email belongs to another entity")
        void shouldThrowDuplicateEntityException_whenEmailBelongsToAnotherEntity() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            MinorStudentDataModel existing = buildExistingEntity(pii);

            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            TutorDataModel.TutorCompositeId tutorCompositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, MinorStudentUpdateUseCase.MAP_NAME);
            when(tutorRepository.findById(tutorCompositeId)).thenReturn(Optional.of(new TutorDataModel()));
            doNothing().when(modelMapper).map(dto, pii);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(MINOR_STUDENT_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when phone belongs to another entity")
        void shouldThrowDuplicateEntityException_whenPhoneBelongsToAnotherEntity() {
            // Given
            MinorStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            MinorStudentDataModel existing = buildExistingEntity(pii);

            MinorStudentDataModel.MinorStudentCompositeId compositeId =
                    new MinorStudentDataModel.MinorStudentCompositeId(TENANT_ID, MINOR_STUDENT_ID);
            TutorDataModel.TutorCompositeId tutorCompositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(minorStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, MinorStudentUpdateUseCase.MAP_NAME);
            when(tutorRepository.findById(tutorCompositeId)).thenReturn(Optional.of(new TutorDataModel()));
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(MINOR_STUDENT_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.MINOR_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });
        }
    }
}
