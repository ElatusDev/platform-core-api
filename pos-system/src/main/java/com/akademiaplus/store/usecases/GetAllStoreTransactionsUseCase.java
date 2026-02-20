/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.usecases;

import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all store transactions in the current tenant.
 */
@Service
public class GetAllStoreTransactionsUseCase {

    private final StoreTransactionRepository storeTransactionRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllStoreTransactionsUseCase with the required dependencies.
     *
     * @param storeTransactionRepository the repository for store transaction data access
     * @param modelMapper                the mapper for entity-to-DTO conversion
     */
    public GetAllStoreTransactionsUseCase(StoreTransactionRepository storeTransactionRepository,
                                          ModelMapper modelMapper) {
        this.storeTransactionRepository = storeTransactionRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all store transactions for the current tenant context.
     *
     * @return a list of store transaction response DTOs
     */
    public List<GetStoreTransactionResponseDTO> getAll() {
        return storeTransactionRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetStoreTransactionResponseDTO.class))
                .toList();
    }
}
