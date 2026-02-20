/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link StoreSaleItemDataModel} entities.
 */
@Repository
public interface StoreSaleItemRepository extends TenantScopedRepository<StoreSaleItemDataModel, StoreSaleItemDataModel.SaleItemCompositeId> {
}
