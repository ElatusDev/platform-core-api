/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.exception.StoreProductNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a store product by its identifier within the current tenant.
 */
@Service
public class GetStoreProductByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final StoreProductRepository storeProductRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetStoreProductByIdUseCase with the required dependencies.
     *
     * @param storeProductRepository the repository for store product data access
     * @param tenantContextHolder    the holder for the current tenant context
     * @param modelMapper            the mapper for entity-to-DTO conversion
     */
    public GetStoreProductByIdUseCase(StoreProductRepository storeProductRepository,
                                      TenantContextHolder tenantContextHolder,
                                      ModelMapper modelMapper) {
        this.storeProductRepository = storeProductRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a store product by its identifier within the current tenant context.
     *
     * @param storeProductId the unique identifier of the store product
     * @return the store product response DTO
     * @throws IllegalArgumentException         if tenant context is not available
     * @throws StoreProductNotFoundException    if no store product is found with the given identifier
     */
    public GetStoreProductResponseDTO get(Long storeProductId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<StoreProductDataModel> queryResult = storeProductRepository.findById(
                new StoreProductDataModel.ProductCompositeId(tenantId, storeProductId));
        if (queryResult.isPresent()) {
            StoreProductDataModel found = queryResult.get();
            return modelMapper.map(found, GetStoreProductResponseDTO.class);
        } else {
            throw new StoreProductNotFoundException(String.valueOf(storeProductId));
        }
    }
}
