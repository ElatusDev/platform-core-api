/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.tenancy.TenantBrandingDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantBrandingRepository extends TenantScopedRepository<TenantBrandingDataModel, Long> {
}
