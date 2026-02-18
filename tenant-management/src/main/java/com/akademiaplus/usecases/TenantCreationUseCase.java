package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles tenant creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Unlike people entities, tenant is not tenant-scoped (it IS the tenant),
 * so there is no PII, auth, or sequential-ID layer involved. The DB
 * generates the {@code tenant_id} via {@code AUTO_INCREMENT}.
 */
@Component
@RequiredArgsConstructor
public class TenantCreationUseCase {

    private final TenantRepository tenantRepository;
    private final ApplicationContext applicationContext;

    /**
     * Creates and persists a new tenant from the given request.
     *
     * @param dto the tenant creation request
     * @return the persisted tenant data model with the generated ID
     */
    @Transactional
    public TenantDataModel create(TenantCreateRequestDTO dto) {
        return tenantRepository.save(transform(dto));
    }

    /**
     * Maps a {@link TenantCreateRequestDTO} to a {@link TenantDataModel}.
     * <p>
     * Uses prototype-scoped bean retrieval to ensure a fresh entity instance,
     * following the same pattern as {@code EmployeeCreationUseCase.transform()}.
     *
     * @param dto the tenant creation request
     * @return a detached data model ready for persistence
     */
    public TenantDataModel transform(TenantCreateRequestDTO dto) {
        TenantDataModel model = applicationContext.getBean(TenantDataModel.class);
        model.setOrganizationName(dto.getOrganizationName());
        model.setLegalName(dto.getLegalName());
        model.setWebsiteUrl(dto.getWebsiteUrl() != null ? dto.getWebsiteUrl().toString() : null);
        model.setEmail(dto.getEmail());
        model.setAddress(dto.getAddress());
        model.setPhone(dto.getPhone());
        model.setLandline(dto.getLandline());
        model.setDescription(dto.getDescription());
        model.setTaxId(dto.getTaxId());
        return model;
    }
}
