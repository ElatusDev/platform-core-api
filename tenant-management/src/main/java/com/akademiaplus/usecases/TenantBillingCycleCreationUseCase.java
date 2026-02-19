/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles tenant billing cycle creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class TenantBillingCycleCreationUseCase {
    public static final String MAP_NAME = "tenantBillingCycleMap";

    private final ApplicationContext applicationContext;
    private final TenantBillingCycleRepository repository;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new billing cycle from the given request.
     *
     * @param dto the billing cycle creation request
     * @return the persisted billing cycle mapped to a response DTO
     */
    @Transactional
    public BillingCycleDTO create(BillingCycleCreateRequestDTO dto) {
        TenantBillingCycleDataModel saved = repository.save(transform(dto));
        return modelMapper.map(saved, BillingCycleDTO.class);
    }

    /**
     * Maps a {@link BillingCycleCreateRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into the entity ID. The billing status defaults to PENDING as
     * defined in the entity.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public TenantBillingCycleDataModel transform(BillingCycleCreateRequestDTO dto) {
        final TenantBillingCycleDataModel model =
                applicationContext.getBean(TenantBillingCycleDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }
}
