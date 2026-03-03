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
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.security.dto.RegistrationRequestDTO;
import openapi.akademiaplus.domain.security.dto.RegistrationResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Map;

/**
 * Handles tenant registration by creating a tenant, an admin employee,
 * and issuing a JWT.
 * <p>
 * <b>Transaction strategy:</b> This method is intentionally NOT
 * {@code @Transactional}. Each sub-call ({@code TenantCreationUseCase},
 * {@code EmployeeCreationUseCase}) manages its own transaction. The tenant
 * must be committed before the employee is created because
 * {@code SequentialIDGenerator.generateId()} uses
 * {@code Propagation.REQUIRES_NEW} to insert into {@code tenant_sequences},
 * which has a FK to {@code tenants}. If both run inside a single outer
 * transaction, the REQUIRES_NEW child transaction deadlocks waiting for the
 * parent's uncommitted tenant row.
 * <p>
 * This use case lives in the {@code application} module (not {@code security})
 * to avoid adding cross-module dependencies from security to
 * tenant-management and user-management.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class RegistrationUseCase {

    public static final String ADMIN_EMPLOYEE_TYPE = "ADMINISTRATOR";
    public static final String ADMIN_ROLE = "ADMIN";

    private final com.akademiaplus.usecases.TenantCreationUseCase tenantCreationUseCase;
    private final EmployeeCreationUseCase employeeCreationUseCase;
    private final TenantContextHolder tenantContextHolder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new tenant with an admin employee and returns a JWT.
     * <p>
     * Flow:
     * <ol>
     *   <li>Create the tenant (commits independently)</li>
     *   <li>Set tenant context so the employee is scoped correctly</li>
     *   <li>Create the admin employee (commits independently)</li>
     *   <li>Issue a JWT for the new admin</li>
     * </ol>
     *
     * @param request the registration request containing tenant and admin details
     * @return a response containing the JWT and the new tenant ID
     */
    public RegistrationResponseDTO register(RegistrationRequestDTO request) {
        // 1. Build and create tenant
        TenantCreateRequestDTO tenantDto = buildTenantRequest(request);
        TenantDTO tenant = tenantCreationUseCase.create(tenantDto);
        Long tenantId = tenant.getTenantId();

        // 2. CRITICAL: set tenant context before creating employee
        tenantContextHolder.setTenantId(tenantId);

        // 3. Build and create admin employee
        EmployeeCreationRequestDTO employeeDto = buildEmployeeRequest(request);
        employeeCreationUseCase.create(employeeDto);

        // 4. Issue JWT
        Map<String, Object> additionalClaims = Map.of(
                "role", ADMIN_ROLE
        );
        String token = jwtTokenProvider.createToken(
                request.getUsername(), tenantId, additionalClaims);

        // 5. Build response
        RegistrationResponseDTO response = new RegistrationResponseDTO();
        response.setToken(token);
        response.setTenantId(tenantId);
        return response;
    }

    private TenantCreateRequestDTO buildTenantRequest(RegistrationRequestDTO request) {
        TenantCreateRequestDTO dto = new TenantCreateRequestDTO();
        dto.setOrganizationName(request.getOrganizationName());
        dto.setEmail(request.getEmail());
        dto.setAddress(request.getAddress());
        return dto;
    }

    private EmployeeCreationRequestDTO buildEmployeeRequest(RegistrationRequestDTO request) {
        EmployeeCreationRequestDTO dto = new EmployeeCreationRequestDTO();
        dto.setFirstName(request.getFirstName());
        dto.setLastName(request.getLastName());
        dto.setEmail(request.getEmail());
        dto.setPhoneNumber(request.getPhone());
        dto.setAddress(request.getAddress());
        dto.setZipCode(request.getZipCode());
        dto.setBirthdate(request.getBirthdate());
        dto.setUsername(request.getUsername());
        dto.setPassword(request.getPassword());
        dto.setEmployeeType(ADMIN_EMPLOYEE_TYPE);
        dto.setRole(ADMIN_ROLE);
        dto.setEntryDate(LocalDate.now());
        return dto;
    }
}
