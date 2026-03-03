/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.store.usecases.StoreProductCreationUseCase;
import com.akademiaplus.store.usecases.StoreTransactionCreationUseCase;
import com.akademiaplus.store.usecases.UpdateStoreProductUseCase;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for POS system DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into entity ID fields
 * and read-only FK relationships.
 */
@Configuration
public class PosModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public PosModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerStoreProductMap();
        registerStoreProductUpdateMap();
        registerStoreTransactionMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerStoreProductMap() {
        modelMapper.createTypeMap(
                StoreProductCreationRequestDTO.class,
                StoreProductDataModel.class,
                StoreProductCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(StoreProductDataModel::setStoreProductId);
        }).implicitMappings();
    }

    private void registerStoreProductUpdateMap() {
        modelMapper.createTypeMap(
                StoreProductCreationRequestDTO.class,
                StoreProductDataModel.class,
                UpdateStoreProductUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(StoreProductDataModel::setStoreProductId);
        }).implicitMappings();
    }

    private void registerStoreTransactionMap() {
        modelMapper.createTypeMap(
                StoreTransactionCreationRequestDTO.class,
                StoreTransactionDataModel.class,
                StoreTransactionCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(StoreTransactionDataModel::setStoreTransactionId);
            mapper.skip(StoreTransactionDataModel::setEmployee);
            mapper.skip(StoreTransactionDataModel::setSaleItems);
            mapper.skip(StoreTransactionDataModel::setTransactionDatetime);
        }).implicitMappings();
    }
}
