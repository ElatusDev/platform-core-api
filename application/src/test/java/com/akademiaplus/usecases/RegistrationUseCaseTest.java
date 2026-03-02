/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import openapi.akademiaplus.domain.security.dto.RegistrationRequestDTO;
import openapi.akademiaplus.domain.security.dto.RegistrationResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("RegistrationUseCase")
@ExtendWith(MockitoExtension.class)
class RegistrationUseCaseTest {

    public static final String TEST_ORG_NAME = "Test Organization";
    public static final String TEST_EMAIL = "admin@test.com";
    public static final String TEST_ADDRESS = "123 Test St";
    public static final String TEST_FIRST_NAME = "John";
    public static final String TEST_LAST_NAME = "Doe";
    public static final String TEST_PHONE = "5551234567";
    public static final String TEST_ZIP_CODE = "12345";
    public static final LocalDate TEST_BIRTHDATE = LocalDate.of(1990, 1, 15);
    public static final String TEST_USERNAME = "johndoe";
    public static final String TEST_PASSWORD = "SecurePass1!";
    public static final Long TEST_TENANT_ID = 1L;
    public static final String TEST_TOKEN = "jwt-token-value";

    @Mock private TenantCreationUseCase tenantCreationUseCase;
    @Mock private EmployeeCreationUseCase employeeCreationUseCase;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @Captor private ArgumentCaptor<TenantCreateRequestDTO> tenantDtoCaptor;
    @Captor private ArgumentCaptor<EmployeeCreationRequestDTO> employeeDtoCaptor;

    private RegistrationUseCase registrationUseCase;

    @BeforeEach
    void setUp() {
        registrationUseCase = new RegistrationUseCase(
                tenantCreationUseCase,
                employeeCreationUseCase,
                tenantContextHolder,
                jwtTokenProvider
        );
    }

    private RegistrationRequestDTO buildRequest() {
        RegistrationRequestDTO dto = new RegistrationRequestDTO();
        dto.setOrganizationName(TEST_ORG_NAME);
        dto.setEmail(TEST_EMAIL);
        dto.setAddress(TEST_ADDRESS);
        dto.setFirstName(TEST_FIRST_NAME);
        dto.setLastName(TEST_LAST_NAME);
        dto.setPhone(TEST_PHONE);
        dto.setZipCode(TEST_ZIP_CODE);
        dto.setBirthdate(TEST_BIRTHDATE);
        dto.setUsername(TEST_USERNAME);
        dto.setPassword(TEST_PASSWORD);
        return dto;
    }

    private void stubHappyPath() {
        TenantDTO tenantResult = new TenantDTO();
        tenantResult.setTenantId(TEST_TENANT_ID);
        when(tenantCreationUseCase.create(tenantDtoCaptor.capture())).thenReturn(tenantResult);
        when(employeeCreationUseCase.create(employeeDtoCaptor.capture()))
                .thenReturn(new EmployeeCreationResponseDTO());
        when(jwtTokenProvider.createToken(
                eq(TEST_USERNAME), eq(TEST_TENANT_ID),
                eq(Map.of("role", RegistrationUseCase.ADMIN_ROLE))))
                .thenReturn(TEST_TOKEN);
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Should create tenant, employee, and return JWT when valid request")
        void shouldCreateTenantEmployeeAndReturnJwt_whenValidRequest() {
            // Given
            RegistrationRequestDTO request = buildRequest();
            stubHappyPath();

            // When
            RegistrationResponseDTO response = registrationUseCase.register(request);

            // Then
            assertThat(response.getToken()).isEqualTo(TEST_TOKEN);
            assertThat(response.getTenantId()).isEqualTo(TEST_TENANT_ID);
            verify(tenantCreationUseCase).create(tenantDtoCaptor.getValue());
            verify(employeeCreationUseCase).create(employeeDtoCaptor.getValue());
        }

        @Test
        @DisplayName("Should set tenant context after tenant creation and before employee creation")
        void shouldSetTenantContext_whenTenantCreated() {
            // Given
            RegistrationRequestDTO request = buildRequest();
            stubHappyPath();

            // When
            registrationUseCase.register(request);

            // Then
            InOrder inOrder = inOrder(tenantCreationUseCase, tenantContextHolder, employeeCreationUseCase);
            inOrder.verify(tenantCreationUseCase).create(tenantDtoCaptor.getValue());
            inOrder.verify(tenantContextHolder).setTenantId(TEST_TENANT_ID);
            inOrder.verify(employeeCreationUseCase).create(employeeDtoCaptor.getValue());
        }

        @Test
        @DisplayName("Should pass ADMINISTRATOR type and ADMIN role to employee creation")
        void shouldPassAdminTypeAndRole_whenCreatingEmployee() {
            // Given
            RegistrationRequestDTO request = buildRequest();
            stubHappyPath();

            // When
            registrationUseCase.register(request);

            // Then
            EmployeeCreationRequestDTO captured = employeeDtoCaptor.getValue();
            assertThat(captured.getEmployeeType()).isEqualTo(RegistrationUseCase.ADMIN_EMPLOYEE_TYPE);
            assertThat(captured.getRole()).isEqualTo(RegistrationUseCase.ADMIN_ROLE);
        }

        @Test
        @DisplayName("Should map registration fields to tenant request DTO")
        void shouldMapFieldsToTenantRequest_whenRegistering() {
            // Given
            RegistrationRequestDTO request = buildRequest();
            stubHappyPath();

            // When
            registrationUseCase.register(request);

            // Then
            TenantCreateRequestDTO captured = tenantDtoCaptor.getValue();
            assertThat(captured.getOrganizationName()).isEqualTo(TEST_ORG_NAME);
            assertThat(captured.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(captured.getAddress()).isEqualTo(TEST_ADDRESS);
        }

        @Test
        @DisplayName("Should map registration fields to employee request DTO")
        void shouldMapFieldsToEmployeeRequest_whenRegistering() {
            // Given
            RegistrationRequestDTO request = buildRequest();
            stubHappyPath();

            // When
            registrationUseCase.register(request);

            // Then
            EmployeeCreationRequestDTO captured = employeeDtoCaptor.getValue();
            assertThat(captured.getFirstName()).isEqualTo(TEST_FIRST_NAME);
            assertThat(captured.getLastName()).isEqualTo(TEST_LAST_NAME);
            assertThat(captured.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(captured.getPhoneNumber()).isEqualTo(TEST_PHONE);
            assertThat(captured.getAddress()).isEqualTo(TEST_ADDRESS);
            assertThat(captured.getZipCode()).isEqualTo(TEST_ZIP_CODE);
            assertThat(captured.getBirthdate()).isEqualTo(TEST_BIRTHDATE);
            assertThat(captured.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(captured.getPassword()).isEqualTo(TEST_PASSWORD);
        }
    }
}
