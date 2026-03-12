/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.tenant;

import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock tenant billing cycle records into the database.
 */
@Service
public class LoadTenantBillingCycleMockDataUseCase
        extends AbstractMockDataUseCase<BillingCycleCreateRequestDTO, TenantBillingCycleDataModel,
        TenantBillingCycleDataModel.TenantBillingCycleCompositeId> {

    public LoadTenantBillingCycleMockDataUseCase(
            DataLoader<BillingCycleCreateRequestDTO, TenantBillingCycleDataModel,
                    TenantBillingCycleDataModel.TenantBillingCycleCompositeId> dataLoader,
            @Qualifier("tenantBillingCycleDataCleanUp")
            DataCleanUp<TenantBillingCycleDataModel,
                    TenantBillingCycleDataModel.TenantBillingCycleCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
