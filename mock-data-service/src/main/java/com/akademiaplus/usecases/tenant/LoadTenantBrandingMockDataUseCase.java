/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.tenant;

import com.akademiaplus.tenancy.TenantBrandingDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock tenant branding records into the database.
 */
@Service
public class LoadTenantBrandingMockDataUseCase
        extends AbstractMockDataUseCase<TenantBrandingDataModel, TenantBrandingDataModel, Long> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the data loader for tenant branding records
     * @param dataCleanUp the data cleanup for the tenant_branding table
     */
    public LoadTenantBrandingMockDataUseCase(
            DataLoader<TenantBrandingDataModel, TenantBrandingDataModel, Long> dataLoader,
            @Qualifier("tenantBrandingDataCleanUp")
            DataCleanUp<TenantBrandingDataModel, Long> dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
