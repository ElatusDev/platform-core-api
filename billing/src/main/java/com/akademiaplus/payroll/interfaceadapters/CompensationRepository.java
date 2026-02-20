/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payroll.interfaceadapters;

import com.akademiaplus.billing.payroll.CompensationDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompensationRepository extends TenantScopedRepository<CompensationDataModel, CompensationDataModel.CompensationCompositeId> {
}
