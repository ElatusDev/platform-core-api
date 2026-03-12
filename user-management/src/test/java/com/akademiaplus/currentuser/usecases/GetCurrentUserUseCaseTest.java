/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.currentuser.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.security.HashingService;
import openapi.akademiaplus.domain.user.management.dto.GetCurrentUserResponseDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GetCurrentUserUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("GetCurrentUserUseCase")
@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseTest {

    private static final String USERNAME = "john.doe";
    private static final String USERNAME_HASH = "hashed_john_doe";
    private static final Long INTERNAL_AUTH_ID = 10L;
    private static final Long EMPLOYEE_ID = 100L;
    private static final Long COLLABORATOR_ID = 200L;
    private static final String ROLE = "ADMIN";
    private static final String EMPLOYEE_TYPE = "INSTRUCTOR";
    private static final String SKILLS = "Mathematics, Physics";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String EMAIL = "john.doe@example.com";
    private static final String PHONE_NUMBER = "+1-555-123-4567";
    private static final String ADDRESS = "123 Main St";
    private static final String ZIP_CODE = "12345";
    private static final LocalDate BIRTHDATE = LocalDate.of(1990, 5, 15);
    private static final LocalDate ENTRY_DATE = LocalDate.of(2025, 1, 15);

    @Mock
    private InternalAuthRepository internalAuthRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CollaboratorRepository collaboratorRepository;

    @Mock
    private HashingService hashingService;

    private GetCurrentUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCurrentUserUseCase(
                internalAuthRepository,
                employeeRepository,
                collaboratorRepository,
                hashingService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Employee Resolution")
    class EmployeeResolution {

        @Test
        @DisplayName("Should return employee profile when authenticated user is an employee")
        void shouldReturnEmployeeProfile_whenAuthenticatedUserIsEmployee() {
            // Given
            setAuthentication(USERNAME);
            InternalAuthDataModel auth = buildAuth();
            EmployeeDataModel employee = buildEmployee();

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(auth));
            when(employeeRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.of(employee));

            // When
            GetCurrentUserResponseDTO result = useCase.getCurrentUser();

            // Then
            assertThat(result.getUserType())
                    .isEqualTo(GetCurrentUserResponseDTO.UserTypeEnum.EMPLOYEE);
            assertThat(result.getEmployeeId().get()).isEqualTo(EMPLOYEE_ID);
            assertThat(result.getInternalAuthId()).isEqualTo(INTERNAL_AUTH_ID);
            assertThat(result.getUsername()).isEqualTo(USERNAME);
            assertThat(result.getRole()).isEqualTo(ROLE);
            assertThat(result.getEmployeeType().get()).isEqualTo(EMPLOYEE_TYPE);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getBirthdate()).isEqualTo(BIRTHDATE);
            assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE);

            InOrder inOrder = inOrder(hashingService, internalAuthRepository, employeeRepository);
            inOrder.verify(hashingService, times(1)).generateHash(USERNAME);
            inOrder.verify(internalAuthRepository, times(1)).findByUsernameHash(USERNAME_HASH);
            inOrder.verify(employeeRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(collaboratorRepository);
        }
    }

    @Nested
    @DisplayName("Collaborator Resolution")
    class CollaboratorResolution {

        @Test
        @DisplayName("Should return collaborator profile when authenticated user is a collaborator")
        void shouldReturnCollaboratorProfile_whenAuthenticatedUserIsCollaborator() {
            // Given
            setAuthentication(USERNAME);
            InternalAuthDataModel auth = buildAuth();
            CollaboratorDataModel collaborator = buildCollaborator();

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(auth));
            when(employeeRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.empty());
            when(collaboratorRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.of(collaborator));

            // When
            GetCurrentUserResponseDTO result = useCase.getCurrentUser();

            // Then
            assertThat(result.getUserType())
                    .isEqualTo(GetCurrentUserResponseDTO.UserTypeEnum.COLLABORATOR);
            assertThat(result.getCollaboratorId().get()).isEqualTo(COLLABORATOR_ID);
            assertThat(result.getInternalAuthId()).isEqualTo(INTERNAL_AUTH_ID);
            assertThat(result.getUsername()).isEqualTo(USERNAME);
            assertThat(result.getRole()).isEqualTo(ROLE);
            assertThat(result.getSkills().get()).isEqualTo(SKILLS);
            assertThat(result.getFirstName()).isEqualTo(FIRST_NAME);
            assertThat(result.getLastName()).isEqualTo(LAST_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getBirthdate()).isEqualTo(BIRTHDATE);
            assertThat(result.getEntryDate()).isEqualTo(ENTRY_DATE);

            InOrder inOrder = inOrder(hashingService, internalAuthRepository, employeeRepository, collaboratorRepository);
            inOrder.verify(hashingService, times(1)).generateHash(USERNAME);
            inOrder.verify(internalAuthRepository, times(1)).findByUsernameHash(USERNAME_HASH);
            inOrder.verify(employeeRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verify(collaboratorRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Error Cases")
    class ErrorCases {

        @Test
        @DisplayName("Should throw IllegalStateException when no authentication is present")
        void shouldThrowIllegalStateException_whenNoAuthenticationPresent() {
            // Given — no authentication set

            // When / Then
            assertThatThrownBy(() -> useCase.getCurrentUser())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(GetCurrentUserUseCase.ERROR_NO_AUTHENTICATION);

            verifyNoInteractions(hashingService, internalAuthRepository, employeeRepository, collaboratorRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when internal auth not found")
        void shouldThrowEntityNotFoundException_whenInternalAuthNotFound() {
            // Given
            setAuthentication(USERNAME);

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.getCurrentUser())
                    .isInstanceOf(EntityNotFoundException.class);

            InOrder inOrder = inOrder(hashingService, internalAuthRepository);
            inOrder.verify(hashingService, times(1)).generateHash(USERNAME);
            inOrder.verify(internalAuthRepository, times(1)).findByUsernameHash(USERNAME_HASH);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(employeeRepository, collaboratorRepository);
        }

        @Test
        @DisplayName("Should throw EntityNotFoundException when no user profile matches the auth")
        void shouldThrowEntityNotFoundException_whenNoUserProfileMatchesAuth() {
            // Given
            setAuthentication(USERNAME);
            InternalAuthDataModel auth = buildAuth();

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(auth));
            when(employeeRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.empty());
            when(collaboratorRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.getCurrentUser())
                    .isInstanceOf(EntityNotFoundException.class);

            InOrder inOrder = inOrder(hashingService, internalAuthRepository, employeeRepository, collaboratorRepository);
            inOrder.verify(hashingService, times(1)).generateHash(USERNAME);
            inOrder.verify(internalAuthRepository, times(1)).findByUsernameHash(USERNAME_HASH);
            inOrder.verify(employeeRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verify(collaboratorRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("PII Mapping")
    class PiiMapping {

        @Test
        @DisplayName("Should handle null PersonPII gracefully")
        void shouldHandleNullPersonPii_gracefully() {
            // Given
            setAuthentication(USERNAME);
            InternalAuthDataModel auth = buildAuth();
            EmployeeDataModel employee = buildEmployee();
            employee.setPersonPII(null);

            when(hashingService.generateHash(USERNAME)).thenReturn(USERNAME_HASH);
            when(internalAuthRepository.findByUsernameHash(USERNAME_HASH))
                    .thenReturn(Optional.of(auth));
            when(employeeRepository.findByInternalAuthId(INTERNAL_AUTH_ID))
                    .thenReturn(Optional.of(employee));

            // When
            GetCurrentUserResponseDTO result = useCase.getCurrentUser();

            // Then
            assertThat(result.getUserType())
                    .isEqualTo(GetCurrentUserResponseDTO.UserTypeEnum.EMPLOYEE);
            assertThat(result.getFirstName()).isNull();
            assertThat(result.getLastName()).isNull();
            assertThat(result.getEmail()).isNull();

            InOrder inOrder = inOrder(hashingService, internalAuthRepository, employeeRepository);
            inOrder.verify(hashingService, times(1)).generateHash(USERNAME);
            inOrder.verify(internalAuthRepository, times(1)).findByUsernameHash(USERNAME_HASH);
            inOrder.verify(employeeRepository, times(1)).findByInternalAuthId(INTERNAL_AUTH_ID);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(collaboratorRepository);
        }
    }

    private void setAuthentication(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private InternalAuthDataModel buildAuth() {
        InternalAuthDataModel auth = new InternalAuthDataModel();
        auth.setInternalAuthId(INTERNAL_AUTH_ID);
        auth.setUsername(USERNAME);
        auth.setRole(ROLE);
        auth.setUsernameHash(USERNAME_HASH);
        return auth;
    }

    private EmployeeDataModel buildEmployee() {
        EmployeeDataModel employee = new EmployeeDataModel();
        employee.setEmployeeId(EMPLOYEE_ID);
        employee.setInternalAuthId(INTERNAL_AUTH_ID);
        employee.setEmployeeType(EMPLOYEE_TYPE);
        employee.setBirthDate(BIRTHDATE);
        employee.setEntryDate(ENTRY_DATE);
        employee.setPersonPII(buildPersonPii());
        return employee;
    }

    private CollaboratorDataModel buildCollaborator() {
        CollaboratorDataModel collaborator = new CollaboratorDataModel();
        collaborator.setCollaboratorId(COLLABORATOR_ID);
        collaborator.setInternalAuthId(INTERNAL_AUTH_ID);
        collaborator.setSkills(SKILLS);
        collaborator.setBirthDate(BIRTHDATE);
        collaborator.setEntryDate(ENTRY_DATE);
        collaborator.setPersonPII(buildPersonPii());
        return collaborator;
    }

    private PersonPIIDataModel buildPersonPii() {
        PersonPIIDataModel pii = new PersonPIIDataModel();
        pii.setFirstName(FIRST_NAME);
        pii.setLastName(LAST_NAME);
        pii.setEmail(EMAIL);
        pii.setPhoneNumber(PHONE_NUMBER);
        pii.setAddress(ADDRESS);
        pii.setZipCode(ZIP_CODE);
        return pii;
    }
}
