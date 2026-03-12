/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AdultStudentUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class AdultStudentUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long ADULT_STUDENT_ID = 42L;
    private static final Long PERSON_PII_ID = 99L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";

    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private AdultStudentUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdultStudentUpdateUseCase(
                adultStudentRepository,
                personPIIRepository,
                tenantContextHolder,
                modelMapper,
                piiNormalizer,
                hashingService
        );
    }

    private AdultStudentUpdateRequestDTO buildDto() {
        AdultStudentUpdateRequestDTO dto = new AdultStudentUpdateRequestDTO();
        dto.setBirthdate(LocalDate.of(1990, 1, 15));
        dto.setFirstName("John");
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

    private AdultStudentDataModel buildExistingEntity(PersonPIIDataModel pii) {
        AdultStudentDataModel entity = new AdultStudentDataModel();
        entity.setPersonPII(pii);
        return entity;
    }

    private void stubPiiRehash(PersonPIIDataModel pii) {
        when(piiNormalizer.normalizeEmail(pii.getEmail())).thenReturn(NORMALIZED_EMAIL);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber())).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when adult student does not exist")
        void shouldThrowEntityNotFoundException_whenAdultStudentDoesNotExist() {
            // Given
            AdultStudentUpdateRequestDTO dto = buildDto();
            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(ADULT_STUDENT_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(ADULT_STUDENT_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(personPIIRepository, modelMapper, piiNormalizer, hashingService);
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update entity and return response when adult student exists")
        void shouldUpdateEntityAndReturnResponse_whenAdultStudentExists() {
            // Given
            AdultStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            AdultStudentDataModel existing = buildExistingEntity(pii);
            AdultStudentDataModel saved = new AdultStudentDataModel();

            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(adultStudentRepository.saveAndFlush(existing)).thenReturn(saved);

            // When
            AdultStudentUpdateResponseDTO result = useCase.update(ADULT_STUDENT_ID, dto);

            // Then
            assertThat(result.getAdultStudentId()).isEqualTo(ADULT_STUDENT_ID);
            assertThat(result.getMessage()).isEqualTo(AdultStudentUpdateUseCase.UPDATE_SUCCESS_MESSAGE);

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(adultStudentRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("PII Update")
    class PiiUpdate {

        @Test
        @DisplayName("Should rehash email and phone when PII fields are updated")
        void shouldRehashEmailAndPhone_whenPiiFieldsAreUpdated() {
            // Given
            AdultStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            AdultStudentDataModel existing = buildExistingEntity(pii);
            AdultStudentDataModel saved = new AdultStudentDataModel();

            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(adultStudentRepository.saveAndFlush(existing)).thenReturn(saved);

            // When
            useCase.update(ADULT_STUDENT_ID, dto);

            // Then
            assertThat(pii.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(pii.getPhoneHash()).isEqualTo(PHONE_HASH);

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(adultStudentRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Duplicate Validation")
    class DuplicateValidation {

        @Test
        @DisplayName("Should throw DuplicateEntityException when email belongs to another entity")
        void shouldThrowDuplicateEntityException_whenEmailBelongsToAnotherEntity() {
            // Given
            AdultStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            AdultStudentDataModel existing = buildExistingEntity(pii);

            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(ADULT_STUDENT_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when phone belongs to another entity")
        void shouldThrowDuplicateEntityException_whenPhoneBelongsToAnotherEntity() {
            // Given
            AdultStudentUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            AdultStudentDataModel existing = buildExistingEntity(pii);

            AdultStudentDataModel.AdultStudentCompositeId compositeId =
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(ADULT_STUDENT_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, adultStudentRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, AdultStudentUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
