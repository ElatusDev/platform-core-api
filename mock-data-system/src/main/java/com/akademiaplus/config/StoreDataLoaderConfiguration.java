/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.billing.store.StoreSaleItemDataModel;
import com.akademiaplus.billing.store.StoreProductDataModel;
import com.akademiaplus.billing.store.StoreTransactionDataModel;
import com.akademiaplus.store.interfaceadapters.StoreSaleItemRepository;
import com.akademiaplus.store.interfaceadapters.StoreProductRepository;
import com.akademiaplus.store.interfaceadapters.StoreTransactionRepository;
import com.akademiaplus.store.usecases.StoreProductCreationUseCase;
import com.akademiaplus.store.usecases.StoreTransactionCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.mock.store.StoreSaleItemFactory.StoreSaleItemRequest;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.pos.system.dto.StoreProductCreationRequestDTO;
import openapi.akademiaplus.domain.pos.system.dto.StoreTransactionCreationRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for POS-related mock data loader and cleanup beans.
 */
@Configuration
public class StoreDataLoaderConfiguration {

    // ── StoreProduct ──

    @Bean
    public DataLoader<StoreProductCreationRequestDTO, StoreProductDataModel, StoreProductDataModel.ProductCompositeId> storeProductDataLoader(
            StoreProductRepository repository,
            DataFactory<StoreProductCreationRequestDTO> storeProductFactory,
            StoreProductCreationUseCase storeProductCreationUseCase) {

        return new DataLoader<>(repository, storeProductCreationUseCase::transform, storeProductFactory);
    }

    @Bean
    public DataCleanUp<StoreProductDataModel, StoreProductDataModel.ProductCompositeId> storeProductDataCleanUp(
            EntityManager entityManager,
            StoreProductRepository repository) {

        DataCleanUp<StoreProductDataModel, StoreProductDataModel.ProductCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(StoreProductDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── StoreTransaction ──

    @Bean
    public DataLoader<StoreTransactionCreationRequestDTO, StoreTransactionDataModel, StoreTransactionDataModel.StoreTransactionCompositeId> storeTransactionDataLoader(
            StoreTransactionRepository repository,
            DataFactory<StoreTransactionCreationRequestDTO> storeTransactionFactory,
            StoreTransactionCreationUseCase storeTransactionCreationUseCase) {

        return new DataLoader<>(repository, storeTransactionCreationUseCase::transform, storeTransactionFactory);
    }

    @Bean
    public DataCleanUp<StoreTransactionDataModel, StoreTransactionDataModel.StoreTransactionCompositeId> storeTransactionDataCleanUp(
            EntityManager entityManager,
            StoreTransactionRepository repository) {

        DataCleanUp<StoreTransactionDataModel, StoreTransactionDataModel.StoreTransactionCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(StoreTransactionDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── StoreSaleItem (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<StoreSaleItemRequest, StoreSaleItemDataModel, StoreSaleItemDataModel.SaleItemCompositeId>
            storeSaleItemDataLoader(
                    StoreSaleItemRepository repository,
                    DataFactory<StoreSaleItemRequest> factory,
                    ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            StoreSaleItemDataModel model = applicationContext.getBean(StoreSaleItemDataModel.class);
            model.setStoreTransactionId(dto.storeTransactionId());
            model.setStoreProductId(dto.storeProductId());
            model.setQuantity(dto.quantity());
            model.setUnitPriceAtSale(dto.unitPriceAtSale());
            model.setItemTotal(dto.itemTotal());
            return model;
        }, factory);
    }

    @Bean
    public DataCleanUp<StoreSaleItemDataModel, StoreSaleItemDataModel.SaleItemCompositeId> storeSaleItemDataCleanUp(
            EntityManager entityManager,
            StoreSaleItemRepository repository) {

        DataCleanUp<StoreSaleItemDataModel, StoreSaleItemDataModel.SaleItemCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(StoreSaleItemDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
