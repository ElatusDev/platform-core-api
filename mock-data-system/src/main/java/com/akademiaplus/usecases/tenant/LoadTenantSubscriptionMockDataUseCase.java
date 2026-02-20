/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.tenant;

import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock tenant subscription records into the database.
 */
@Service
public class LoadTenantSubscriptionMockDataUseCase
        extends AbstractMockDataUseCase<SubscriptionCreateRequestDTO, TenantSubscriptionDataModel,
        TenantSubscriptionDataModel.TenantSubscriptionCompositeId> {

    public LoadTenantSubscriptionMockDataUseCase(
            DataLoader<SubscriptionCreateRequestDTO, TenantSubscriptionDataModel,
                    TenantSubscriptionDataModel.TenantSubscriptionCompositeId> dataLoader,
            @Qualifier("tenantSubscriptionDataCleanUp")
            DataCleanUp<TenantSubscriptionDataModel,
                    TenantSubscriptionDataModel.TenantSubscriptionCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
