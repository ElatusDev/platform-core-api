/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerAuthRepository extends TenantScopedRepository<CustomerAuthDataModel, CustomerAuthDataModel.CustomerAuthCompositeId> {
}
