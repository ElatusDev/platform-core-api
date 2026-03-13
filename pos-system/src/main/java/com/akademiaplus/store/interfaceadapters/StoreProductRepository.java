/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreProductRepository extends TenantScopedRepository<StoreProductDataModel, StoreProductDataModel.ProductCompositeId> {

    /**
     * Finds active products for catalog display, optionally filtered by category.
     * Returns only products with stock > 0 and not soft-deleted.
     *
     * @param category optional category filter (null returns all active products)
     * @return catalog-eligible products
     */
    @Query("SELECT p FROM StoreProductDataModel p WHERE p.deletedAt IS NULL "
         + "AND p.stockQuantity > 0 "
         + "AND (:category IS NULL OR p.category = :category) "
         + "ORDER BY p.name ASC")
    List<StoreProductDataModel> findCatalogProducts(@Param("category") String category);
}
