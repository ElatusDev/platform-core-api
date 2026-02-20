/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantBillingCycleRepository
        extends TenantScopedRepository<TenantBillingCycleDataModel, TenantBillingCycleDataModel.TenantBillingCycleCompositeId> {
}
