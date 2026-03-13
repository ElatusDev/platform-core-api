/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.pos.system.dto.GetCatalogItemResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

/**
 * Use case for retrieving a single catalog item by product ID.
 */
@Service
public class GetCatalogItemByIdUseCase {

    private final StoreProductRepository storeProductRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    public GetCatalogItemByIdUseCase(StoreProductRepository storeProductRepository,
                                     TenantContextHolder tenantContextHolder,
                                     ModelMapper modelMapper) {
        this.storeProductRepository = storeProductRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a catalog item by its product ID within the current tenant context.
     *
     * @param storeProductId the product ID
     * @return the catalog item response DTO
     */
    public GetCatalogItemResponseDTO get(Long storeProductId) {
        Long tenantId = tenantContextHolder.requireTenantId();

        StoreProductDataModel entity = storeProductRepository
                .findById(new StoreProductDataModel.ProductCompositeId(tenantId, storeProductId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.STORE_PRODUCT, String.valueOf(storeProductId)));

        GetCatalogItemResponseDTO dto = modelMapper.map(entity, GetCatalogItemResponseDTO.class);
        dto.setInStock(entity.getStockQuantity() > 0);
        return dto;
    }
}
