/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.leadmanagement;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import com.akademiaplus.util.base.AbstractPlatformMockDataUseCase;
import com.akademiaplus.util.base.PlatformDataCleanUp;
import com.akademiaplus.util.base.PlatformDataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock demo request records into the database.
 */
@Service
public class LoadDemoRequestMockDataUseCase
        extends AbstractPlatformMockDataUseCase<DemoRequestDataModel, DemoRequestDataModel> {

    /**
     * Constructs the use case with the demo-request-specific data loader and cleanup.
     *
     * @param dataLoader  the platform data loader for demo requests
     * @param dataCleanup the platform data cleanup for demo requests
     */
    public LoadDemoRequestMockDataUseCase(
            PlatformDataLoader<DemoRequestDataModel, DemoRequestDataModel> dataLoader,
            @Qualifier("demoRequestDataCleanUp")
            PlatformDataCleanUp<DemoRequestDataModel> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
