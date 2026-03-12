/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("CollaboratorCreationUseCase")
@ExtendWith(MockitoExtension.class)
class CollaboratorCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private CollaboratorRepository repository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;

    private CollaboratorCreationUseCase useCase;

    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";
    private static final String USERNAME_HASH = "hashed_username";

    @BeforeEach
    void setUp() {
        useCase = new CollaboratorCreationUseCase(
                applicationContext,
                repository,
                personPIIRepository,
                modelMapper,
                hashingService,
                piiNormalizer
        );
    }

    private CollaboratorCreationRequestDTO buildDto() {
        return new CollaboratorCreationRequestDTO(
                "Java, Spring Boot",
                LocalDate.of(1990, 1, 15),
                LocalDate.of(2024, 1, 1),
                "John", "Doe",
                TEST_EMAIL, TEST_PHONE,
                "123 Main St", "12345",
                "jdoe", "securePass123", "COLLABORATOR"
        );
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel personPII = new PersonPIIDataModel();
        personPII.setEmail(TEST_EMAIL);
        personPII.setPhoneNumber(TEST_PHONE);
        return personPII;
    }

    private void stubTransform(CollaboratorCreationRequestDTO dto,
                               CollaboratorDataModel model,
                               PersonPIIDataModel personPII) {
        InternalAuthDataModel internalAuth = new InternalAuthDataModel();
        internalAuth.setUsername("jdoe");

        when(applicationContext.getBean(InternalAuthDataModel.class)).thenReturn(internalAuth);
        doNothing().when(modelMapper).map(dto, internalAuth);
        when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
        doNothing().when(modelMapper).map(dto, personPII);
        when(applicationContext.getBean(CollaboratorDataModel.class)).thenReturn(model);
        doNothing().when(modelMapper).map(dto, model, CollaboratorCreationUseCase.TYPE_MAP);
        when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
        when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
        when(hashingService.generateHash("jdoe")).thenReturn(USERNAME_HASH);
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save and return response when creating collaborator")
        void shouldSaveAndReturnResponse_whenCreatingCollaborator() {
            // Given
            CollaboratorCreationRequestDTO dto = buildDto();
            CollaboratorDataModel model = new CollaboratorDataModel();
            PersonPIIDataModel personPII = buildPersonPII();
            CollaboratorDataModel savedModel = new CollaboratorDataModel();
            CollaboratorCreationResponseDTO expectedResponse = new CollaboratorCreationResponseDTO();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(false);
            when(repository.saveAndFlush(model)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, CollaboratorCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            CollaboratorCreationResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result).isEqualTo(expectedResponse);

            InOrder inOrder = inOrder(applicationContext, modelMapper, piiNormalizer, hashingService,
                    personPIIRepository, repository);
            inOrder.verify(applicationContext, times(1)).getBean(InternalAuthDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, model.getInternalAuth());
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, personPII);
            inOrder.verify(applicationContext, times(1)).getBean(CollaboratorDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, model, CollaboratorCreationUseCase.TYPE_MAP);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_EMAIL);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(TEST_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verify(hashingService, times(1)).generateHash("jdoe");
            inOrder.verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            inOrder.verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
            inOrder.verify(repository, times(1)).saveAndFlush(model);
            inOrder.verify(modelMapper, times(1)).map(savedModel, CollaboratorCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Duplicate validation")
    class DuplicateValidation {

        @Test
        @DisplayName("Should throw DuplicateEntityException when email already exists")
        void shouldThrowDuplicateEntityException_whenEmailAlreadyExists() {
            // Given
            CollaboratorCreationRequestDTO dto = buildDto();
            CollaboratorDataModel model = new CollaboratorDataModel();
            PersonPIIDataModel personPII = buildPersonPII();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.EMAIL + " for " + EntityType.COLLABORATOR)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when phone already exists")
        void shouldThrowDuplicateEntityException_whenPhoneAlreadyExists() {
            // Given
            CollaboratorCreationRequestDTO dto = buildDto();
            CollaboratorDataModel model = new CollaboratorDataModel();
            PersonPIIDataModel personPII = buildPersonPII();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .hasMessage("Duplicate " + PiiField.PHONE_NUMBER + " for " + EntityType.COLLABORATOR)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.COLLABORATOR);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });

            verify(personPIIRepository, times(1)).existsByEmailHash(EMAIL_HASH);
            verify(personPIIRepository, times(1)).existsByPhoneHash(PHONE_HASH);
            verifyNoInteractions(repository);
        }
    }
}
