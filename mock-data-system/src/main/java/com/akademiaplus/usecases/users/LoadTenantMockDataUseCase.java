/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.users;

import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads mock tenant records into the database.
 * <p>
 * Tenants must be loaded and <strong>committed</strong> before any
 * tenant-scoped entity because
 * {@link com.akademiaplus.utilities.idgeneration.SequentialIDGenerator}
 * uses {@code Propagation.REQUIRES_NEW}. That independent transaction
 * inserts into {@code tenant_sequences}, which has an FK to
 * {@code tenants.tenant_id} — so the tenant row must already be
 * visible (committed) when the FK check runs.
 * <p>
 * Overriding {@link #load(int)} with {@code REQUIRES_NEW} guarantees
 * the tenant INSERT commits independently of the caller's transaction.
 */
@Service
public class LoadTenantMockDataUseCase extends AbstractMockDataUseCase<TenantCreateRequestDTO, TenantDataModel, Long> {

    public LoadTenantMockDataUseCase(DataLoader<TenantCreateRequestDTO, TenantDataModel, Long> dataLoader,
                                     @Qualifier("tenantDataCleanUp") DataCleanUp<TenantDataModel, Long> dataCleanup) {
        super(dataLoader, dataCleanup);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void load(int count) {
        super.load(count);
    }
}
