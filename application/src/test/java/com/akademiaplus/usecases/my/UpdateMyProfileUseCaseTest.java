/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.my.dto.MyProfileResponseDTO;
import openapi.akademiaplus.domain.my.dto.UpdateMyProfileRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("UpdateMyProfileUseCase")
@ExtendWith(MockitoExtension.class)
class UpdateMyProfileUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PROFILE_ID = 100L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john@test.com";
    private static final String PHONE = "555-1234";
    private static final String NORMALIZED_PHONE = "5551234";
    private static final String PHONE_HASH = "hash_555";
    private static final String ADDRESS = "123 Main St";
    private static final String ZIP = "12345";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 15);
    private static final LocalDate ENTRY_DATE = LocalDate.of(2025, 6, 1);

    @Mock private UserContextHolder userContextHolder;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private HashingService hashingService;

    private UpdateMyProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateMyProfileUseCase(userContextHolder, tenantContextHolder,
                adultStudentRepository, tutorRepository, collaboratorRepository,
                piiNormalizer, hashingService);
    }

    private PersonPIIDataModel buildPii() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setFirstName(FIRST_NAME);
        pii.setLastName(LAST_NAME);
        pii.setEmail(EMAIL);
        pii.setPhoneNumber(PHONE);
        pii.setAddress(ADDRESS);
        pii.setZipCode(ZIP);
        return pii;
    }

    private AdultStudentDataModel buildAdultStudent() {
        AdultStudentDataModel student = new AdultStudentDataModel();
        student.setPersonPII(buildPii());
        student.setBirthDate(BIRTH_DATE);
        student.setEntryDate(ENTRY_DATE);
        return student;
    }

    @Nested
    @DisplayName("Update Adult Student Profile")
    class UpdateAdultStudentProfile {

        @Test
        @DisplayName("Should update first name and return updated DTO")
        void shouldUpdateFirstName_whenProvided() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            AdultStudentDataModel student = buildAdultStudent();
            when(adultStudentRepository.findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.of(student));

            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setFirstName("Jane");

            // When
            MyProfileResponseDTO result = useCase.execute(request);

            // Then — state
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getProfileId()).isEqualTo(PROFILE_ID);
            assertThat(result.getProfileType()).isEqualTo(MyProfileResponseDTO.ProfileTypeEnum.ADULT_STUDENT);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, tenantContextHolder, adultStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(tutorRepository, piiNormalizer, hashingService);
        }

        @Test
        @DisplayName("Should normalize phone and generate hash when phone provided")
        void shouldNormalizePhoneAndHash_whenPhoneProvided() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            AdultStudentDataModel student = buildAdultStudent();
            when(adultStudentRepository.findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.of(student));
            when(piiNormalizer.normalizePhoneNumber(PHONE)).thenReturn(NORMALIZED_PHONE);
            when(hashingService.generateHash(NORMALIZED_PHONE)).thenReturn(PHONE_HASH);

            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setPhoneNumber(PHONE);

            // When
            MyProfileResponseDTO result = useCase.execute(request);

            // Then — state
            assertThat(result.getPhoneNumber()).isEqualTo(NORMALIZED_PHONE);

            // Then — interactions
            InOrder inOrder = inOrder(piiNormalizer, hashingService);
            inOrder.verify(piiNormalizer, times(1)).normalizePhoneNumber(PHONE);
            inOrder.verify(hashingService, times(1)).generateHash(NORMALIZED_PHONE);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when adult student not found")
        void shouldThrowEntityNotFoundException_whenAdultStudentNotFound() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(adultStudentRepository.findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.empty());

            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setFirstName("Jane");

            // When / Then
            assertThatThrownBy(() -> useCase.execute(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            EntityType.ADULT_STUDENT, PROFILE_ID.toString()));

            verifyNoInteractions(tutorRepository, piiNormalizer, hashingService);
        }
    }

    @Nested
    @DisplayName("Update Tutor Profile")
    class UpdateTutorProfile {

        @Test
        @DisplayName("Should update address and zipCode for tutor")
        void shouldUpdateAddressAndZip_whenTutorExists() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_TUTOR);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            TutorDataModel tutor = new TutorDataModel();
            tutor.setPersonPII(buildPii());
            tutor.setBirthDate(BIRTH_DATE);
            tutor.setEntryDate(ENTRY_DATE);
            when(tutorRepository.findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.of(tutor));

            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setAddress("456 Oak Ave");
            request.setZipCode("67890");

            // When
            MyProfileResponseDTO result = useCase.execute(request);

            // Then — state
            assertThat(result.getAddress()).isEqualTo("456 Oak Ave");
            assertThat(result.getZipCode()).isEqualTo("67890");
            assertThat(result.getProfileType()).isEqualTo(MyProfileResponseDTO.ProfileTypeEnum.TUTOR);

            // Then — interactions
            verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, PROFILE_ID));
            verifyNoInteractions(adultStudentRepository, piiNormalizer, hashingService);
        }
    }

    @Nested
    @DisplayName("Unsupported Profile Type")
    class UnsupportedProfileType {

        @Test
        @DisplayName("Should throw EntityNotFoundException when profile type is unknown")
        void shouldThrowEntityNotFoundException_whenProfileTypeUnknown() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn("UNKNOWN");
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();

            // When / Then
            assertThatThrownBy(() -> useCase.execute(request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessage(String.format(EntityNotFoundException.MESSAGE_TEMPLATE,
                            UpdateMyProfileUseCase.ERROR_UNSUPPORTED_PROFILE_TYPE, "UNKNOWN"));

            verifyNoInteractions(adultStudentRepository, tutorRepository, piiNormalizer, hashingService);
        }
    }
}
