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
import openapi.akademiaplus.domain.pos.system.dto.SaleItemRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link StoreTransactionCreationRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class StoreTransactionFactory implements DataFactory<StoreTransactionCreationRequestDTO> {

    private final StoreTransactionDataGenerator generator;

    @Override
    public List<StoreTransactionCreationRequestDTO> generate(int count) {
        List<StoreTransactionCreationRequestDTO> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            transactions.add(createTransaction());
        }
        return transactions;
    }

    private StoreTransactionCreationRequestDTO createTransaction() {
        StoreTransactionCreationRequestDTO dto = new StoreTransactionCreationRequestDTO();
        dto.setTransactionType(generator.transactionType());
        dto.setPaymentMethod(generator.paymentMethod());
        dto.setSaleItems(generateSaleItems());
        return dto;
    }

    private List<SaleItemRequestDTO> generateSaleItems() {
        int count = generator.saleItemCount();
        List<SaleItemRequestDTO> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SaleItemRequestDTO item = new SaleItemRequestDTO();
            item.setStoreProductId(generator.saleItemStoreProductId());
            item.setQuantity(generator.saleItemQuantity());
            items.add(item);
        }
        return items;
    }
}
