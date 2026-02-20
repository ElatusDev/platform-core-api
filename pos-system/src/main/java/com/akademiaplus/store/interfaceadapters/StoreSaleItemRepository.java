/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for {@link StoreSaleItemDataModel} entities.
 */
@Repository
public interface StoreSaleItemRepository extends JpaRepository<StoreSaleItemDataModel, Long> {
}
