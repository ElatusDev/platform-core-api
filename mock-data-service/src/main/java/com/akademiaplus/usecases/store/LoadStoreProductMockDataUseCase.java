/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.store;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock store product records into the database.
 */
@Service
public class LoadStoreProductMockDataUseCase
        extends AbstractMockDataUseCase<StoreProductCreationRequestDTO, StoreProductDataModel, StoreProductDataModel.ProductCompositeId> {

    public LoadStoreProductMockDataUseCase(
            DataLoader<StoreProductCreationRequestDTO, StoreProductDataModel, StoreProductDataModel.ProductCompositeId> dataLoader,
            @Qualifier("storeProductDataCleanUp")
            DataCleanUp<StoreProductDataModel, StoreProductDataModel.ProductCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
