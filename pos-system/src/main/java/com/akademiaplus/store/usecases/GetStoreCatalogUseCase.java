/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetCatalogItemResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving the public store catalog.
 * Returns active products with stock > 0, optionally filtered by category.
 */
@Service
public class GetStoreCatalogUseCase {

    private final StoreProductRepository storeProductRepository;
    private final ModelMapper modelMapper;

    public GetStoreCatalogUseCase(StoreProductRepository storeProductRepository,
                                  ModelMapper modelMapper) {
        this.storeProductRepository = storeProductRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves the catalog of active products for the current tenant.
     *
     * @param category optional category filter (null returns all)
     * @return list of catalog item DTOs
     */
    public List<GetCatalogItemResponseDTO> getCatalog(String category) {
        return storeProductRepository.findCatalogProducts(category).stream()
                .map(entity -> {
                    GetCatalogItemResponseDTO dto = modelMapper.map(entity, GetCatalogItemResponseDTO.class);
                    dto.setInStock(entity.getStockQuantity() > 0);
                    return dto;
                })
                .toList();
    }
}
