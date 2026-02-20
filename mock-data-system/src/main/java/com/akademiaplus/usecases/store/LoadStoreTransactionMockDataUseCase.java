/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.store;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock store transaction records into the database.
 */
@Service
public class LoadStoreTransactionMockDataUseCase
        extends AbstractMockDataUseCase<StoreTransactionCreationRequestDTO, StoreTransactionDataModel, Long> {

    public LoadStoreTransactionMockDataUseCase(
            DataLoader<StoreTransactionCreationRequestDTO, StoreTransactionDataModel, Long> dataLoader,
            @Qualifier("storeTransactionDataCleanUp")
            DataCleanUp<StoreTransactionDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
