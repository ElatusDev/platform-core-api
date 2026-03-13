/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.usecases.GetTenantBrandingUseCase;
import com.akademiaplus.usecases.UpdateTenantBrandingUseCase;
import openapi.akademiaplus.domain.tenant.management.api.TenantApi;
import openapi.akademiaplus.domain.tenant.management.dto.GetTenantBrandingResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantBrandingUpdateRequestDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for tenant branding configuration operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
public class TenantBrandingController implements TenantApi {

    private final GetTenantBrandingUseCase getTenantBrandingUseCase;
    private final UpdateTenantBrandingUseCase updateTenantBrandingUseCase;

    public TenantBrandingController(GetTenantBrandingUseCase getTenantBrandingUseCase,
                                    UpdateTenantBrandingUseCase updateTenantBrandingUseCase) {
        this.getTenantBrandingUseCase = getTenantBrandingUseCase;
        this.updateTenantBrandingUseCase = updateTenantBrandingUseCase;
    }

    @Override
    public ResponseEntity<GetTenantBrandingResponseDTO> getTenantBranding() {
        return ResponseEntity.ok(getTenantBrandingUseCase.get());
    }

    @Override
    public ResponseEntity<GetTenantBrandingResponseDTO> updateTenantBranding(
            TenantBrandingUpdateRequestDTO tenantBrandingUpdateRequestDTO) {
        return ResponseEntity.ok(
                updateTenantBrandingUseCase.upsert(tenantBrandingUpdateRequestDTO));
    }
}
