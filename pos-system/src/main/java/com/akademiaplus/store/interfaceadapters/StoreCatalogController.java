/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.store.usecases.GetCatalogItemByIdUseCase;
import com.akademiaplus.store.usecases.GetStoreCatalogUseCase;
import openapi.akademiaplus.domain.pos.system.api.StoreApi;
import openapi.akademiaplus.domain.pos.system.dto.GetCatalogItemResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for public store catalog operations.
 * Provides read-only access to active products for catalog display.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/pos-system")
public class StoreCatalogController implements StoreApi {

    private final GetStoreCatalogUseCase getStoreCatalogUseCase;
    private final GetCatalogItemByIdUseCase getCatalogItemByIdUseCase;

    public StoreCatalogController(GetStoreCatalogUseCase getStoreCatalogUseCase,
                                  GetCatalogItemByIdUseCase getCatalogItemByIdUseCase) {
        this.getStoreCatalogUseCase = getStoreCatalogUseCase;
        this.getCatalogItemByIdUseCase = getCatalogItemByIdUseCase;
    }

    @Override
    public ResponseEntity<List<GetCatalogItemResponseDTO>> getStoreCatalog(
            String category, Integer page, Integer size) {
        return ResponseEntity.ok(getStoreCatalogUseCase.getCatalog(category));
    }

    @Override
    public ResponseEntity<GetCatalogItemResponseDTO> getCatalogItemById(Long storeProductId) {
        return ResponseEntity.ok(getCatalogItemByIdUseCase.get(storeProductId));
    }
}
