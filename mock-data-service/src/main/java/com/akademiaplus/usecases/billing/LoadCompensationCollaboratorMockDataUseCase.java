/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.billing;

import com.akademiaplus.util.base.AbstractBridgeMockDataUseCase;
import com.akademiaplus.util.base.NativeBridgeDataCleanUp;
import com.akademiaplus.util.base.NativeBridgeDataLoader;
import com.akademiaplus.util.mock.billing.CompensationCollaboratorRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock compensation-to-collaborator bridge records into the database.
 */
@Service
public class LoadCompensationCollaboratorMockDataUseCase
        extends AbstractBridgeMockDataUseCase<CompensationCollaboratorRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for compensation-collaborator records
     * @param dataCleanUp the native bridge data cleanup for the compensation_collaborators table
     */
    public LoadCompensationCollaboratorMockDataUseCase(
            NativeBridgeDataLoader<CompensationCollaboratorRecord> dataLoader,
            @Qualifier("compensationCollaboratorDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
