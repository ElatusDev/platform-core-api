/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.store;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link StoreProductCreationRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class StoreProductFactory implements DataFactory<StoreProductCreationRequestDTO> {

    private final StoreProductDataGenerator generator;

    @Override
    public List<StoreProductCreationRequestDTO> generate(int count) {
        List<StoreProductCreationRequestDTO> products = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            products.add(createProduct());
        }
        return products;
    }

    private StoreProductCreationRequestDTO createProduct() {
        StoreProductCreationRequestDTO dto = new StoreProductCreationRequestDTO();
        dto.setName(generator.name());
        dto.setDescription(generator.description());
        dto.setPrice(generator.price());
        dto.setStockQuantity(generator.stockQuantity());
        return dto;
    }
}
