/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorUpdateResponseDTO;
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

@DisplayName("CollaboratorUpdateUseCase")
@ExtendWith(MockitoExtension.class)
class CollaboratorUpdateUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long COLLABORATOR_ID = 42L;
    private static final Long PERSON_PII_ID = 99L;
    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";
    private static final String TEST_ROLE = "COLLABORATOR";

    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private CollaboratorUpdateUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CollaboratorUpdateUseCase(
                collaboratorRepository,
                personPIIRepository,
                tenantContextHolder,
                modelMapper,
                piiNormalizer,
                hashingService
        );
    }

    private CollaboratorUpdateRequestDTO buildDto() {
        CollaboratorUpdateRequestDTO dto = new CollaboratorUpdateRequestDTO();
        dto.setSkills("Java,Spring");
        dto.setBirthdate(LocalDate.of(1990, 1, 15));
        dto.setEntryDate(LocalDate.of(2025, 1, 15));
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setEmail(TEST_EMAIL);
        dto.setPhoneNumber(TEST_PHONE);
        dto.setAddress("123 Main St");
        dto.setZipCode("12345");
        dto.setRole(TEST_ROLE);
        return dto;
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setPersonPiiId(PERSON_PII_ID);
        pii.setEmail(TEST_EMAIL);
        pii.setPhoneNumber(TEST_PHONE);
        return pii;
    }

    private CollaboratorDataModel buildExistingEntity(PersonPIIDataModel pii) {
        CollaboratorDataModel entity = new CollaboratorDataModel();
        entity.setPersonPII(pii);
        entity.setInternalAuth(new InternalAuthDataModel());
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
        @DisplayName("Should throw EntityNotFoundException when collaborator does not exist")
        void shouldThrowEntityNotFoundException_whenCollaboratorDoesNotExist() {
            // Given
            CollaboratorUpdateRequestDTO dto = buildDto();
            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> useCase.update(COLLABORATOR_ID, dto))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.COLLABORATOR, COLLABORATOR_ID))
                    .satisfies(ex -> {
                        EntityNotFoundException enfe = (EntityNotFoundException) ex;
                        assertThat(enfe.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(enfe.getEntityId()).isEqualTo(String.valueOf(COLLABORATOR_ID));
                    });

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(personPIIRepository, modelMapper, piiNormalizer, hashingService);
        }
    }

    @Nested
    @DisplayName("Successful Update")
    class SuccessfulUpdate {

        @Test
        @DisplayName("Should update entity and return response when collaborator exists")
        void shouldUpdateEntityAndReturnResponse_whenCollaboratorExists() {
            // Given
            CollaboratorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CollaboratorDataModel existing = buildExistingEntity(pii);
            CollaboratorDataModel saved = new CollaboratorDataModel();

            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(collaboratorRepository.saveAndFlush(existing)).thenReturn(saved);

            // When
            CollaboratorUpdateResponseDTO result = useCase.update(COLLABORATOR_ID, dto);

            // Then
            assertThat(result.getCollaboratorId()).isEqualTo(COLLABORATOR_ID);
            assertThat(result.getMessage()).isEqualTo(CollaboratorUpdateUseCase.UPDATE_SUCCESS_MESSAGE);

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(collaboratorRepository, times(1)).saveAndFlush(existing);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Auth Update")
    class AuthUpdate {

        @Test
        @DisplayName("Should update role on internal auth when collaborator exists")
        void shouldUpdateRole_whenCollaboratorExists() {
            // Given
            CollaboratorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CollaboratorDataModel existing = buildExistingEntity(pii);
            InternalAuthDataModel auth = existing.getInternalAuth();
            CollaboratorDataModel saved = new CollaboratorDataModel();

            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(collaboratorRepository.saveAndFlush(existing)).thenReturn(saved);

            // When
            useCase.update(COLLABORATOR_ID, dto);

            // Then
            assertThat(auth.getRole()).isEqualTo(TEST_ROLE);

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(collaboratorRepository, times(1)).saveAndFlush(existing);
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
            CollaboratorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CollaboratorDataModel existing = buildExistingEntity(pii);
            CollaboratorDataModel saved = new CollaboratorDataModel();

            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(collaboratorRepository.saveAndFlush(existing)).thenReturn(saved);

            // When
            useCase.update(COLLABORATOR_ID, dto);

            // Then
            assertThat(pii.getEmailHash()).isEqualTo(EMAIL_HASH);
            assertThat(pii.getPhoneHash()).isEqualTo(PHONE_HASH);

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            inOrder.verify(modelMapper, times(1)).map(dto, pii);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID);
            inOrder.verify(collaboratorRepository, times(1)).saveAndFlush(existing);
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
            CollaboratorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CollaboratorDataModel existing = buildExistingEntity(pii);

            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
            when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(COLLABORATOR_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage(String.format(DuplicateEntityException.MESSAGE_TEMPLATE,
                            PiiField.EMAIL, EntityType.COLLABORATOR))
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
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
            CollaboratorUpdateRequestDTO dto = buildDto();
            PersonPIIDataModel pii = buildPersonPII();
            CollaboratorDataModel existing = buildExistingEntity(pii);

            CollaboratorDataModel.CollaboratorCompositeId compositeId =
                    new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, COLLABORATOR_ID);

            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(collaboratorRepository.findById(compositeId)).thenReturn(Optional.of(existing));
            doNothing().when(modelMapper).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
            doNothing().when(modelMapper).map(dto, pii);
            stubPiiRehash(pii);
            when(personPIIRepository.existsByEmailHashAndPersonPiiIdNot(EMAIL_HASH, PERSON_PII_ID))
                    .thenReturn(false);
            when(personPIIRepository.existsByPhoneHashAndPersonPiiIdNot(PHONE_HASH, PERSON_PII_ID))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.update(COLLABORATOR_ID, dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage(String.format(DuplicateEntityException.MESSAGE_TEMPLATE,
                            PiiField.PHONE_NUMBER, EntityType.COLLABORATOR))
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            InOrder inOrder = inOrder(tenantContextHolder, collaboratorRepository, modelMapper,
                    piiNormalizer, hashingService, personPIIRepository);
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(collaboratorRepository, times(1)).findById(compositeId);
            inOrder.verify(modelMapper, times(1)).map(dto, existing, CollaboratorUpdateUseCase.MAP_NAME);
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
