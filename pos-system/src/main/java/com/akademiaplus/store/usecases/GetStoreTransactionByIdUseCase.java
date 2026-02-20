/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.exception.StoreTransactionNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a store transaction by its identifier within the current tenant.
 */
@Service
public class GetStoreTransactionByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final StoreTransactionRepository storeTransactionRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetStoreTransactionByIdUseCase with the required dependencies.
     *
     * @param storeTransactionRepository the repository for store transaction data access
     * @param tenantContextHolder        the holder for the current tenant context
     * @param modelMapper                the mapper for entity-to-DTO conversion
     */
    public GetStoreTransactionByIdUseCase(StoreTransactionRepository storeTransactionRepository,
                                          TenantContextHolder tenantContextHolder,
                                          ModelMapper modelMapper) {
        this.storeTransactionRepository = storeTransactionRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a store transaction by its identifier within the current tenant context.
     *
     * @param storeTransactionId the unique identifier of the store transaction
     * @return the store transaction response DTO
     * @throws IllegalArgumentException              if tenant context is not available
     * @throws StoreTransactionNotFoundException     if no store transaction is found with the given identifier
     */
    public GetStoreTransactionResponseDTO get(Long storeTransactionId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<StoreTransactionDataModel> queryResult = storeTransactionRepository.findById(
                new StoreTransactionDataModel.StoreTransactionCompositeId(tenantId, storeTransactionId));
        if (queryResult.isPresent()) {
            StoreTransactionDataModel found = queryResult.get();
            return modelMapper.map(found, GetStoreTransactionResponseDTO.class);
        } else {
            throw new StoreTransactionNotFoundException(String.valueOf(storeTransactionId));
        }
    }
}
