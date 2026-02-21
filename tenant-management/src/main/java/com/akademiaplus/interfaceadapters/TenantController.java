/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.usecases.DeleteTenantUseCase;
import com.akademiaplus.usecases.TenantCreationUseCase;
import openapi.akademiaplus.domain.tenant.management.api.TenantsApi;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant lifecycle operations.
 *
 * <p>Implements the OpenAPI-generated {@link TenantsApi} interface,
 * delegating to domain use cases for creation and deletion.
 *
 * <p>Unlike domain-entity controllers, tenant endpoints are not
 * tenant-scoped — the tenant IS the root entity, so there is no
 * {@code X-Tenant-Id} header requirement for these operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
public class TenantController implements TenantsApi {

    private final TenantCreationUseCase tenantCreationUseCase;
    private final DeleteTenantUseCase deleteTenantUseCase;

    /**
     * Constructs the controller with required use cases.
     *
     * @param tenantCreationUseCase the tenant creation use case
     * @param deleteTenantUseCase   the tenant deletion use case
     */
    public TenantController(TenantCreationUseCase tenantCreationUseCase,
                            DeleteTenantUseCase deleteTenantUseCase) {
        this.tenantCreationUseCase = tenantCreationUseCase;
        this.deleteTenantUseCase = deleteTenantUseCase;
    }

    /**
     * Creates a new tenant organization.
     *
     * @param tenantCreateRequestDTO the tenant creation request
     * @return HTTP 201 with the created tenant details
     */
    @Override
    public ResponseEntity<TenantDTO> createTenant(
            TenantCreateRequestDTO tenantCreateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantCreationUseCase.create(tenantCreateRequestDTO));
    }

    /**
     * Soft-deletes a tenant by its ID.
     *
     * @param tenantId the tenant ID
     * @return HTTP 204 on successful deletion
     */
    @Override
    public ResponseEntity<Void> deleteTenant(Long tenantId) {
        deleteTenantUseCase.delete(tenantId);
        return ResponseEntity.noContent().build();
    }
}
