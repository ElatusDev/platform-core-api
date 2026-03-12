package com.akademiaplus.util.mock.tenant;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link TenantCreateRequestDTO} instances with fake data.
 * <p>
 * Follows the same pattern as people factories: produces OpenAPI DTOs that
 * are transformed into data models by the domain use case.
 */
@Component
@RequiredArgsConstructor
public class TenantFactory implements DataFactory<TenantCreateRequestDTO> {

    private final TenantDataGenerator generator;

    @Override
    public List<TenantCreateRequestDTO> generate(int count) {
        List<TenantCreateRequestDTO> tenants = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tenants.add(createTenant());
        }
        return tenants;
    }

    private TenantCreateRequestDTO createTenant() {
        TenantCreateRequestDTO dto = new TenantCreateRequestDTO();
        dto.setOrganizationName(generator.organizationName());
        dto.setLegalName(generator.legalName());
        dto.setWebsiteUrl(generator.websiteUrl());
        dto.setEmail(generator.email());
        dto.setAddress(generator.address());
        dto.setPhone(generator.phone());
        dto.setLandline(generator.landline());
        dto.setDescription(generator.description());
        dto.setTaxId(generator.taxId());
        return dto;
    }
}
