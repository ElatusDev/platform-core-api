/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.my.dto.MyProfileResponseDTO;
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

@DisplayName("GetMyProfileUseCase")
@ExtendWith(MockitoExtension.class)
class GetMyProfileUseCaseTest {

    private static final Long TENANT_ID = 1L;
    private static final Long PROFILE_ID = 100L;
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john@test.com";
    private static final String PHONE = "555-1234";
    private static final String ADDRESS = "123 Main St";
    private static final String ZIP = "12345";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 15);
    private static final LocalDate ENTRY_DATE = LocalDate.of(2025, 6, 1);

    @Mock private UserContextHolder userContextHolder;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private TutorRepository tutorRepository;

    private GetMyProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetMyProfileUseCase(userContextHolder, tenantContextHolder,
                adultStudentRepository, tutorRepository);
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

    @Nested
    @DisplayName("Adult Student Profile")
    class AdultStudentProfile {

        @Test
        @DisplayName("Should return profile DTO when adult student exists")
        void shouldReturnProfileDto_whenAdultStudentExists() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            AdultStudentDataModel student = new AdultStudentDataModel();
            student.setPersonPII(buildPii());
            student.setBirthDate(BIRTH_DATE);
            student.setEntryDate(ENTRY_DATE);
            when(adultStudentRepository.findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.of(student));

            // When
            MyProfileResponseDTO result = useCase.execute();

            // Then — state
            assertThat(result.getProfileId()).isEqualTo(PROFILE_ID);
            assertThat(result.getProfileType()).isEqualTo(MyProfileResponseDTO.ProfileTypeEnum.ADULT_STUDENT);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getPhoneNumber()).isEqualTo(PHONE);
            assertThat(result.getAddress()).isEqualTo(ADDRESS);
            assertThat(result.getZipCode()).isEqualTo(ZIP);
            assertThat(result.getBirthDate()).isEqualTo(BIRTH_DATE);
            assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, tenantContextHolder, adultStudentRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(adultStudentRepository, times(1)).findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(tutorRepository);
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

            // When / Then
            assertThatThrownBy(() -> useCase.execute())
                    .isInstanceOf(EntityNotFoundException.class);

            verify(adultStudentRepository, times(1)).findById(
                    new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, PROFILE_ID));
            verifyNoInteractions(tutorRepository);
        }
    }

    @Nested
    @DisplayName("Tutor Profile")
    class TutorProfile {

        @Test
        @DisplayName("Should return profile DTO when tutor exists")
        void shouldReturnProfileDto_whenTutorExists() {
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

            // When
            MyProfileResponseDTO result = useCase.execute();

            // Then — state
            assertThat(result.getProfileId()).isEqualTo(PROFILE_ID);
            assertThat(result.getProfileType()).isEqualTo(MyProfileResponseDTO.ProfileTypeEnum.TUTOR);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);

            // Then — interactions
            InOrder inOrder = inOrder(userContextHolder, tenantContextHolder, tutorRepository);
            inOrder.verify(userContextHolder, times(1)).requireProfileType();
            inOrder.verify(userContextHolder, times(1)).requireProfileId();
            inOrder.verify(tenantContextHolder, times(1)).requireTenantId();
            inOrder.verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, PROFILE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(adultStudentRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when tutor not found")
        void shouldThrowEntityNotFoundException_whenTutorNotFound() {
            // Given
            when(userContextHolder.requireProfileType()).thenReturn(JwtTokenProvider.PROFILE_TYPE_TUTOR);
            when(userContextHolder.requireProfileId()).thenReturn(PROFILE_ID);
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(tutorRepository.findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, PROFILE_ID)))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.execute())
                    .isInstanceOf(EntityNotFoundException.class);

            verify(tutorRepository, times(1)).findById(
                    new TutorDataModel.TutorCompositeId(TENANT_ID, PROFILE_ID));
            verifyNoInteractions(adultStudentRepository);
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

            // When / Then
            assertThatThrownBy(() -> useCase.execute())
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(adultStudentRepository, tutorRepository);
        }
    }
}
