/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.PiiField;
import com.akademiaplus.utilities.exceptions.DuplicateEntityException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("AdultStudentCreationUseCase")
@ExtendWith(MockitoExtension.class)
class AdultStudentCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;

    private AdultStudentCreationUseCase useCase;

    private static final String TEST_EMAIL = "jdoe@example.com";
    private static final String TEST_PHONE = "5551234567";
    private static final String NORMALIZED_EMAIL = "jdoe@example.com";
    private static final String NORMALIZED_PHONE = "+525551234567";
    private static final String EMAIL_HASH = "hashed_email";
    private static final String PHONE_HASH = "hashed_phone";

    @BeforeEach
    void setUp() {
        useCase = new AdultStudentCreationUseCase(
                applicationContext,
                adultStudentRepository,
                personPIIRepository,
                modelMapper,
                hashingService,
                piiNormalizer
        );
    }

    private AdultStudentCreationRequestDTO buildDto() {
        return new AdultStudentCreationRequestDTO(
                LocalDate.of(1990, 1, 15),
                "John", "Doe",
                TEST_EMAIL, TEST_PHONE,
                "123 Main St", "12345",
                "GOOGLE", "oauth_abc123"
        );
    }

    private PersonPIIDataModel buildPersonPII() {
        PersonPIIDataModel personPII = new PersonPIIDataModel();
        personPII.setEmail(TEST_EMAIL);
        personPII.setPhoneNumber(TEST_PHONE);
        return personPII;
    }

    private void stubTransform(AdultStudentCreationRequestDTO dto,
                               AdultStudentDataModel model,
                               PersonPIIDataModel personPII) {
        when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(personPII);
        doNothing().when(modelMapper).map(dto, personPII);
        when(applicationContext.getBean(AdultStudentDataModel.class)).thenReturn(model);
        doNothing().when(modelMapper).map(dto, model, AdultStudentCreationUseCase.MAP_NAME);
        when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
        when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(NORMALIZED_EMAIL);
        when(piiNormalizer.normalizePhoneNumber(TEST_PHONE)).thenReturn(NORMALIZED_PHONE);
        when(hashingService.generateHash(NORMALIZED_EMAIL)).thenReturn(EMAIL_HASH);
        when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save and return response when creating adult student")
        void shouldSaveAndReturnResponse_whenCreatingAdultStudent() {
            // Given
            AdultStudentCreationRequestDTO dto = buildDto();
            AdultStudentDataModel model = new AdultStudentDataModel();
            PersonPIIDataModel personPII = buildPersonPII();
            AdultStudentDataModel savedModel = new AdultStudentDataModel();
            AdultStudentCreationResponseDTO expectedResponse = new AdultStudentCreationResponseDTO();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(false);
            when(adultStudentRepository.saveAndFlush(model)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, AdultStudentCreationResponseDTO.class)).thenReturn(expectedResponse);

            // When
            AdultStudentCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(adultStudentRepository).saveAndFlush(model);
            assertThat(result).isEqualTo(expectedResponse);
        }
    }

    @Nested
    @DisplayName("Duplicate validation")
    class DuplicateValidation {

        @Test
        @DisplayName("Should throw DuplicateEntityException when email already exists")
        void shouldThrowDuplicateEntityException_whenEmailAlreadyExists() {
            // Given
            AdultStudentCreationRequestDTO dto = buildDto();
            AdultStudentDataModel model = new AdultStudentDataModel();
            PersonPIIDataModel personPII = buildPersonPII();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.EMAIL);
                    });
        }

        @Test
        @DisplayName("Should throw DuplicateEntityException when phone already exists")
        void shouldThrowDuplicateEntityException_whenPhoneAlreadyExists() {
            // Given
            AdultStudentCreationRequestDTO dto = buildDto();
            AdultStudentDataModel model = new AdultStudentDataModel();
            PersonPIIDataModel personPII = buildPersonPII();

            stubTransform(dto, model, personPII);
            when(personPIIRepository.existsByEmailHash(EMAIL_HASH)).thenReturn(false);
            when(personPIIRepository.existsByPhoneHash(PHONE_HASH)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(DuplicateEntityException.class)
                    .satisfies(ex -> {
                        DuplicateEntityException dee = (DuplicateEntityException) ex;
                        assertThat(dee.getEntityType()).isEqualTo(EntityType.ADULT_STUDENT);
                        assertThat(dee.getField()).isEqualTo(PiiField.PHONE_NUMBER);
                    });
        }
    }
}
