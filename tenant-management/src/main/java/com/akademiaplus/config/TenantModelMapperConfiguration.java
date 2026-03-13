/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.tenancy.TenantBrandingDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.usecases.TenantBillingCycleCreationUseCase;
import com.akademiaplus.usecases.TenantCreationUseCase;
import com.akademiaplus.usecases.TenantSubscriptionCreationUseCase;
import com.akademiaplus.usecases.UpdateTenantBrandingUseCase;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantBrandingUpdateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for tenant-management DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into entity ID fields
 * and other unintended targets.
 *
 * @see TenantCreationUseCase
 * @see TenantSubscriptionCreationUseCase
 * @see TenantBillingCycleCreationUseCase
 */
@Configuration
public class TenantModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public TenantModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerTenantMap();
        registerTenantSubscriptionMap();
        registerTenantBillingCycleMap();
        registerTenantBrandingUpdateMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerTenantMap() {
        modelMapper.createTypeMap(
                TenantCreateRequestDTO.class,
                TenantDataModel.class,
                TenantCreationUseCase.MAP_NAME
        ).addMappings(mapper ->
                mapper.skip(TenantDataModel::setTenantId)
        ).implicitMappings();
    }

    private void registerTenantSubscriptionMap() {
        modelMapper.createTypeMap(
                SubscriptionCreateRequestDTO.class,
                TenantSubscriptionDataModel.class,
                TenantSubscriptionCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
                mapper.skip(TenantSubscriptionDataModel::setTenantSubscriptionId);
                mapper.skip(TenantSubscriptionDataModel::setMaxUsers);
        }).implicitMappings();
    }

    private void registerTenantBillingCycleMap() {
        modelMapper.createTypeMap(
                BillingCycleCreateRequestDTO.class,
                TenantBillingCycleDataModel.class,
                TenantBillingCycleCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
                mapper.skip(TenantBillingCycleDataModel::setTenantBillingCycleId);
                mapper.skip(TenantBillingCycleDataModel::setBillingMonth);
        }).implicitMappings();
    }

    private void registerTenantBrandingUpdateMap() {
        modelMapper.createTypeMap(
                TenantBrandingUpdateRequestDTO.class,
                TenantBrandingDataModel.class,
                UpdateTenantBrandingUseCase.MAP_NAME
        ).addMappings(mapper ->
                mapper.skip(TenantBrandingDataModel::setTenantId)
        ).implicitMappings();
    }
}
