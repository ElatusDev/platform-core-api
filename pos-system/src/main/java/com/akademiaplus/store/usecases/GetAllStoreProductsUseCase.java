/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all store products in the current tenant.
 */
@Service
public class GetAllStoreProductsUseCase {

    private final StoreProductRepository storeProductRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllStoreProductsUseCase with the required dependencies.
     *
     * @param storeProductRepository the repository for store product data access
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetAllStoreProductsUseCase(StoreProductRepository storeProductRepository, ModelMapper modelMapper) {
        this.storeProductRepository = storeProductRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all store products for the current tenant context.
     *
     * @return a list of store product response DTOs
     */
    public List<GetStoreProductResponseDTO> getAll() {
        return storeProductRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetStoreProductResponseDTO.class))
                .toList();
    }
}
