/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.store.usecases.GetAllStoreTransactionsUseCase;
import com.akademiaplus.store.usecases.GetStoreTransactionByIdUseCase;
import com.akademiaplus.store.usecases.StoreTransactionCreationUseCase;
import openapi.akademiaplus.domain.pos.system.api.StoreTransactionsApi;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreTransactionResponseDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for store transaction management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/pos-system")
public class StoreTransactionController implements StoreTransactionsApi {

    private final StoreTransactionCreationUseCase storeTransactionCreationUseCase;
    private final GetAllStoreTransactionsUseCase getAllStoreTransactionsUseCase;
    private final GetStoreTransactionByIdUseCase getStoreTransactionByIdUseCase;

    public StoreTransactionController(
            StoreTransactionCreationUseCase storeTransactionCreationUseCase,
            GetAllStoreTransactionsUseCase getAllStoreTransactionsUseCase,
            GetStoreTransactionByIdUseCase getStoreTransactionByIdUseCase) {
        this.storeTransactionCreationUseCase = storeTransactionCreationUseCase;
        this.getAllStoreTransactionsUseCase = getAllStoreTransactionsUseCase;
        this.getStoreTransactionByIdUseCase = getStoreTransactionByIdUseCase;
    }

    @Override
    public ResponseEntity<StoreTransactionCreationResponseDTO> createStoreTransaction(
            StoreTransactionCreationRequestDTO storeTransactionCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeTransactionCreationUseCase.create(storeTransactionCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetStoreTransactionResponseDTO>> getStoreTransactions() {
        return ResponseEntity.ok(getAllStoreTransactionsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetStoreTransactionResponseDTO> getStoreTransactionById(
            Long storeTransactionId) {
        return ResponseEntity.ok(getStoreTransactionByIdUseCase.get(storeTransactionId));
    }
}
