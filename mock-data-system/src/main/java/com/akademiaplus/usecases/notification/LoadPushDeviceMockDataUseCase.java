/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.notification;

import com.akademiaplus.notifications.PushDeviceDataModel;
import com.akademiaplus.util.base.AbstractPlatformMockDataUseCase;
import com.akademiaplus.util.base.PlatformDataCleanUp;
import com.akademiaplus.util.base.PlatformDataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock push device records into the database.
 */
@Service
public class LoadPushDeviceMockDataUseCase
        extends AbstractPlatformMockDataUseCase<PushDeviceDataModel, PushDeviceDataModel> {

    /**
     * Constructs the use case with the push-device-specific data loader and cleanup.
     *
     * @param dataLoader  the platform data loader for push devices
     * @param dataCleanup the platform data cleanup for push devices
     */
    public LoadPushDeviceMockDataUseCase(
            PlatformDataLoader<PushDeviceDataModel, PushDeviceDataModel> dataLoader,
            @Qualifier("pushDeviceDataCleanUp")
            PlatformDataCleanUp<PushDeviceDataModel> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
