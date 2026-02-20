/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.store;

import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.store.StoreSaleItemFactory.StoreSaleItemRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock store sale item records into the database.
 */
@Service
public class LoadStoreSaleItemMockDataUseCase
        extends AbstractMockDataUseCase<StoreSaleItemRequest, StoreSaleItemDataModel, Long> {

    public LoadStoreSaleItemMockDataUseCase(
            DataLoader<StoreSaleItemRequest, StoreSaleItemDataModel, Long> dataLoader,
            @Qualifier("storeSaleItemDataCleanUp")
            DataCleanUp<StoreSaleItemDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
