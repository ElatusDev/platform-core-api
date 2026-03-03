/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.store.usecases.DeleteStoreProductUseCase;
import com.akademiaplus.store.usecases.GetAllStoreProductsUseCase;
import com.akademiaplus.store.usecases.GetStoreProductByIdUseCase;
import com.akademiaplus.store.usecases.StoreProductCreationUseCase;
import com.akademiaplus.store.usecases.UpdateStoreProductUseCase;
import openapi.akademiaplus.domain.pos.system.api.StoreProductsApi;
import openapi.akademiaplus.domain.pos.system.dto.GetStoreProductResponseDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for store product management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/pos-system")
public class StoreProductController implements StoreProductsApi {

    private final StoreProductCreationUseCase storeProductCreationUseCase;
    private final GetAllStoreProductsUseCase getAllStoreProductsUseCase;
    private final GetStoreProductByIdUseCase getStoreProductByIdUseCase;
    private final DeleteStoreProductUseCase deleteStoreProductUseCase;
    private final UpdateStoreProductUseCase updateStoreProductUseCase;

    public StoreProductController(StoreProductCreationUseCase storeProductCreationUseCase,
                                  GetAllStoreProductsUseCase getAllStoreProductsUseCase,
                                  GetStoreProductByIdUseCase getStoreProductByIdUseCase,
                                  DeleteStoreProductUseCase deleteStoreProductUseCase,
                                  UpdateStoreProductUseCase updateStoreProductUseCase) {
        this.storeProductCreationUseCase = storeProductCreationUseCase;
        this.getAllStoreProductsUseCase = getAllStoreProductsUseCase;
        this.getStoreProductByIdUseCase = getStoreProductByIdUseCase;
        this.deleteStoreProductUseCase = deleteStoreProductUseCase;
        this.updateStoreProductUseCase = updateStoreProductUseCase;
    }

    @Override
    public ResponseEntity<StoreProductCreationResponseDTO> createStoreProduct(
            StoreProductCreationRequestDTO storeProductCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(storeProductCreationUseCase.create(storeProductCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetStoreProductResponseDTO>> getStoreProducts() {
        return ResponseEntity.ok(getAllStoreProductsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetStoreProductResponseDTO> getStoreProductById(Long storeProductId) {
        return ResponseEntity.ok(getStoreProductByIdUseCase.get(storeProductId));
    }

    @Override
    public ResponseEntity<Void> deleteStoreProduct(Long storeProductId) {
        deleteStoreProductUseCase.delete(storeProductId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<StoreProductCreationResponseDTO> updateStoreProduct(
            Long storeProductId, StoreProductCreationRequestDTO storeProductCreationRequestDTO) {
        return ResponseEntity.ok(
                updateStoreProductUseCase.update(storeProductId, storeProductCreationRequestDTO));
    }
}
