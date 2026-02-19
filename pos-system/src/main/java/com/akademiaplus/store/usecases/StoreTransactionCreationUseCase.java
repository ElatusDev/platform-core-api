/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles store transaction creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the read-only FK relationships
 * ({@code employee}, {@code saleItems}).
 */
@Service
@RequiredArgsConstructor
public class StoreTransactionCreationUseCase {
    public static final String MAP_NAME = "storeTransactionMap";

    private final ApplicationContext applicationContext;
    private final StoreTransactionRepository storeTransactionRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public StoreTransactionCreationResponseDTO create(StoreTransactionCreationRequestDTO dto) {
        StoreTransactionDataModel saved = storeTransactionRepository.save(transform(dto));
        return modelMapper.map(saved, StoreTransactionCreationResponseDTO.class);
    }

    public StoreTransactionDataModel transform(StoreTransactionCreationRequestDTO dto) {
        final StoreTransactionDataModel model = applicationContext.getBean(StoreTransactionDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        return model;
    }
}
