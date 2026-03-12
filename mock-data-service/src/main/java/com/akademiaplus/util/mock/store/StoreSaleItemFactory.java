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
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link StoreSaleItemRequest} instances with fake data.
 *
 * <p>Requires store-transaction and store-product IDs to be injected via
 * setters before {@link #generate(int)} is called.</p>
 */
@Component
@RequiredArgsConstructor
public class StoreSaleItemFactory implements DataFactory<StoreSaleItemFactory.StoreSaleItemRequest> {

    /** Error message when store transaction IDs have not been set. */
    public static final String ERROR_TRANSACTION_IDS_NOT_SET =
            "availableStoreTransactionIds must be set before generating sale items";

    /** Error message when store product IDs have not been set. */
    public static final String ERROR_PRODUCT_IDS_NOT_SET =
            "availableStoreProductIds must be set before generating sale items";

    private final StoreSaleItemDataGenerator generator;

    @Setter
    private List<Long> availableStoreTransactionIds = List.of();

    @Setter
    private List<Long> availableStoreProductIds = List.of();

    @Override
    public List<StoreSaleItemRequest> generate(int count) {
        if (availableStoreTransactionIds.isEmpty()) {
            throw new IllegalStateException(ERROR_TRANSACTION_IDS_NOT_SET);
        }
        if (availableStoreProductIds.isEmpty()) {
            throw new IllegalStateException(ERROR_PRODUCT_IDS_NOT_SET);
        }
        List<StoreSaleItemRequest> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long transactionId = availableStoreTransactionIds
                    .get(i % availableStoreTransactionIds.size());
            Long productId = availableStoreProductIds
                    .get(i % availableStoreProductIds.size());
            items.add(createRequest(transactionId, productId));
        }
        return items;
    }

    private StoreSaleItemRequest createRequest(Long transactionId, Long productId) {
        int quantity = generator.quantity();
        BigDecimal unitPrice = generator.unitPriceAtSale();
        BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        return new StoreSaleItemRequest(
                transactionId,
                productId,
                quantity,
                unitPrice,
                itemTotal
        );
    }

    /**
     * Lightweight request record used as the DTO type parameter.
     *
     * @param storeTransactionId FK to the parent transaction
     * @param storeProductId     FK to the product
     * @param quantity           number of units sold
     * @param unitPriceAtSale    price per unit at time of sale
     * @param itemTotal          total for this line item
     */
    public record StoreSaleItemRequest(
            Long storeTransactionId,
            Long storeProductId,
            int quantity,
            BigDecimal unitPriceAtSale,
            BigDecimal itemTotal) { }
}
