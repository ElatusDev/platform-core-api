/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.store.interfaceadapters;

import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for store transaction data access.
 * <p>
 * Overrides {@code findById} and {@code findAll} with {@link EntityGraph}
 * to eagerly load sale items and prevent N+1 queries on read operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface StoreTransactionRepository extends TenantScopedRepository<StoreTransactionDataModel, StoreTransactionDataModel.StoreTransactionCompositeId> {

    /** {@inheritDoc} */
    @EntityGraph(attributePaths = {"saleItems"})
    @Override
    Optional<StoreTransactionDataModel> findById(StoreTransactionDataModel.StoreTransactionCompositeId id);

    /** {@inheritDoc} */
    @EntityGraph(attributePaths = {"saleItems"})
    @Override
    List<StoreTransactionDataModel> findAll();
}
