/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock compensation records into the database.
 */
@Service
public class LoadCompensationMockDataUseCase
        extends AbstractMockDataUseCase<CompensationCreationRequestDTO, CompensationDataModel, CompensationDataModel.CompensationCompositeId> {

    public LoadCompensationMockDataUseCase(
            DataLoader<CompensationCreationRequestDTO, CompensationDataModel, CompensationDataModel.CompensationCompositeId> dataLoader,
            @Qualifier("compensationDataCleanUp")
            DataCleanUp<CompensationDataModel, CompensationDataModel.CompensationCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
