/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.tutor.usecases;

import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.TutorUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorUpdateResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.openapitools.jackson.nullable.JsonNullable;
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

@DisplayName("TutorUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class TutorUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long TUTOR_ID = 42L;
    private static final Long PERSON_PII_ID = 99L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";
    private static final String TEST_PROVIDER = "google";
    private static final String TEST_TOKEN = "oauth-token-123";

    @Mock private TutorRepository tutorRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private TutorUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new TutorUpdateUseCase(
                tutorRepository,
                personPIIRepository,
                tenantContextHolder,
                modelMapper,
                piiNormalizer,
                hashingService
        );
    }

    private TutorUpdateRequestDTO buildDto() {
        TutorUpdateRequestDTO dto = new TutorUpdateRequestDTO();
        dto.setBirthdate(LocalDate.of(1990, 1, 15));
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail(TEST_EMAIL);
        dto.setPhoneNumber(TEST_PHONE);
        dto.setAddress("123 Main St");
        dto.setZipCode("12345");
        dto.setProvider(JsonNullable.of(TEST_PROVIDER));
        dto.setToken(JsonNullable.of(TEST_TOKEN));
        return dto;
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setPersonPiiId(PERSON_PII_ID);
        pii.setEmail(TEST_EMAIL);
        pii.setPhoneNumber(TEST_PHONE);
        return pii;
    }

    private TutorDataModel buildExistingEntity(PersonPIIDataModel pii,
                                                CustomerAuthDataModel auth) {
        TutorDataModel entity = new TutorDataModel();
        entity.setPersonPII(pii);
        entity.setCustomerAuth(auth);
        return entity;
    }

    private void stubPiiRehash(PersonPIIDataModel pii) {
        when(piiNormalizer.normalizeEmail(pii.getEmail())).thenReturn(NORMALIZED_EMAIL);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(piiNormalizer.normalizePhoneNumber(pii.getPhoneNumber())).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
    }

    private void stubFullSuccessPath(TutorUpdateRequestDTO dto,
                                      TutorDataModel existing,
                                      PersonPIIDataModel pii) {
        TutorDataModel.TutorCompositeId compositeId =
                new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

        when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
        when(tutorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
        doNothing().when(modelMapper).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
        doNothing().when(modelMapper).map(dto, pii);
        stubPiiRehash(pii);
        when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                .thenReturn(false);
        when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                .thenReturn(false);
        when(tutorRepository.saveAndFlush(existing)).thenReturn(new TutorDataModel());
    }

    @Nested
    @DisplayName("Entity Lookup")
    class EntityLookup {

        @Test
        @DisplayName("Should throw EntityNotFoundException when tutor does not exist")
        void shouldThrowEntityNotFoundException_whenTutorDoesNotExist() {
            // Given
            TutorUpdateRequestDTO dto = buildDto();
            TutorDataModel.TutorCompositeId compositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tutorRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(TUTOR_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.TUTOR, TUTOR_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(TUTOR_ID));
                    });

            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(tutorRepository, times(1)).findById(compositeId);
            verifyNoMoreInteractions(tenantContextHolder, tutorRepository);
            verifyNoInteractions(personPIIRepository, modelMapper, piiNormalizer, hashingService);
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update entity and return response when tutor exists")
        void shouldUpdateEntityAndReturnResponse_whenTutorExists() {
            // Given
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CustomerAuthDataModel auth = new CustomerAuthDataModel();
            TutorDataModel existing = buildExistingEntity(pii, auth);

            stubFullSuccessPath(dto, existing, pii);

            // When
            TutorUpdateResponseDTO result = useCase.update(TUTOR_ID, dto);

            // Then
            assertThat(result.getTutorId()).isEqualTo(TUTOR_ID);
            assertThat(result.getMessage()).isEqualTo(TutorUpdateUseCase.UPDATE_SUCCESS_MESSAGE);

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(tutorRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Auth Update")
    class AuthUpdate {

        @Test
        @DisplayName("Should update provider and token on customer auth when auth exists")
        void shouldUpdateProviderAndToken_whenAuthExists() {
            // Given
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CustomerAuthDataModel auth = new CustomerAuthDataModel();
            TutorDataModel existing = buildExistingEntity(pii, auth);

            stubFullSuccessPath(dto, existing, pii);

            // When
            useCase.update(TUTOR_ID, dto);

            // Then
            assertThat(auth.getProvider()).isEqualTo(TEST_PROVIDER);
            assertThat(auth.getToken()).isEqualTo(TEST_TOKEN);

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(tutorRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should skip auth update when customer auth is null")
        void shouldSkipAuthUpdate_whenCustomerAuthIsNull() {
            // Given
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            TutorDataModel existing = buildExistingEntity(pii, null);

            stubFullSuccessPath(dto, existing, pii);

            // When
            TutorUpdateResponseDTO result = useCase.update(TUTOR_ID, dto);

            // Then
            assertThat(result.getTutorId()).isEqualTo(TUTOR_ID);
            assertThat(result.getMessage()).isEqualTo(TutorUpdateUseCase.UPDATE_SUCCESS_MESSAGE);

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(tutorRepository, times(1)).saveAndFlush(existing);
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
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CustomerAuthDataModel auth = new CustomerAuthDataModel();
            TutorDataModel existing = buildExistingEntity(pii, auth);

            stubFullSuccessPath(dto, existing, pii);

            // When
            useCase.update(TUTOR_ID, dto);

            // Then
            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(tutorRepository, times(1)).saveAndFlush(existing);
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
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CustomerAuthDataModel auth = new CustomerAuthDataModel();
            TutorDataModel existing = buildExistingEntity(pii, auth);

            TutorDataModel.TutorCompositeId compositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tutorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(TUTOR_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage(String.format(DuplicateEntityException.MESSAGE_TEMPLATE,
                            PiiField.EMAIL, EntityType.TUTOR))
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
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
            TutorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CustomerAuthDataModel auth = new CustomerAuthDataModel();
            TutorDataModel existing = buildExistingEntity(pii, auth);

            TutorDataModel.TutorCompositeId compositeId =
                    new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tutorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(TUTOR_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage(String.format(DuplicateEntityException.MESSAGE_TEMPLATE,
                            PiiField.PHONE_NUMBER, EntityType.TUTOR))
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.TUTOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, tutorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, TutorUpdateUseCase.MAP_NAME);
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
