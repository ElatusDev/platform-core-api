/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles store product creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class StoreProductCreationUseCase {
    public static final String MAP_NAME = "storeProductMap";

    private final ApplicationContext applicationContext;
    private final StoreProductRepository storeProductRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public StoreProductCreationResponseDTO create(StoreProductCreationRequestDTO dto) {
        StoreProductDataModel saved = storeProductRepository.save(transform(dto));
        return modelMapper.map(saved, StoreProductCreationResponseDTO.class);
    }

    public StoreProductDataModel transform(StoreProductCreationRequestDTO dto) {
        final StoreProductDataModel model = applicationContext.getBean(StoreProductDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }
}
