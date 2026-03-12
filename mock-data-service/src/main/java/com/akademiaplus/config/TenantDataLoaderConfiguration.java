/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.interfaceadapters.TenantBillingCycleRepository;
import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.interfaceadapters.TenantSubscriptionRepository;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.usecases.TenantBillingCycleCreationUseCase;
import com.akademiaplus.usecases.TenantCreationUseCase;
import com.akademiaplus.usecases.TenantSubscriptionCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequenceRepository;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for tenant-related mock data loader and cleanup beans.
 */
@Configuration
public class TenantDataLoaderConfiguration {

    // ── Tenant ──

    @Bean
    public DataLoader<TenantCreateRequestDTO, TenantDataModel, Long> tenantDataLoader(
            TenantRepository repository,
            DataFactory<TenantCreateRequestDTO> tenantFactory,
            TenantCreationUseCase tenantCreationUseCase) {

        return new DataLoader<>(repository, tenantCreationUseCase::transform, tenantFactory);
    }

    @Bean
    public DataCleanUp<TenantDataModel, Long> tenantDataCleanUp(
            EntityManager entityManager,
            TenantRepository repository) {

        DataCleanUp<TenantDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TenantDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    @Bean
    public DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> tenantSequenceDataCleanUp(
            EntityManager entityManager,
            TenantSequenceRepository repository) {

        DataCleanUp<TenantSequence, TenantSequence.TenantSequenceId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TenantSequence.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── TenantSubscription ──

    @Bean
    public DataLoader<SubscriptionCreateRequestDTO, TenantSubscriptionDataModel,
            TenantSubscriptionDataModel.TenantSubscriptionCompositeId>
            tenantSubscriptionDataLoader(
                    TenantSubscriptionRepository repository,
                    DataFactory<SubscriptionCreateRequestDTO> tenantSubscriptionFactory,
                    TenantSubscriptionCreationUseCase tenantSubscriptionCreationUseCase) {

        return new DataLoader<>(repository, tenantSubscriptionCreationUseCase::transform, tenantSubscriptionFactory);
    }

    @Bean
    public DataCleanUp<TenantSubscriptionDataModel, TenantSubscriptionDataModel.TenantSubscriptionCompositeId>
            tenantSubscriptionDataCleanUp(
                    EntityManager entityManager,
                    TenantSubscriptionRepository repository) {

        DataCleanUp<TenantSubscriptionDataModel, TenantSubscriptionDataModel.TenantSubscriptionCompositeId> cleanup =
                new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TenantSubscriptionDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── TenantBillingCycle ──

    @Bean
    public DataLoader<BillingCycleCreateRequestDTO, TenantBillingCycleDataModel,
            TenantBillingCycleDataModel.TenantBillingCycleCompositeId>
            tenantBillingCycleDataLoader(
                    TenantBillingCycleRepository repository,
                    DataFactory<BillingCycleCreateRequestDTO> tenantBillingCycleFactory,
                    TenantBillingCycleCreationUseCase tenantBillingCycleCreationUseCase) {

        return new DataLoader<>(repository, tenantBillingCycleCreationUseCase::transform, tenantBillingCycleFactory);
    }

    @Bean
    public DataCleanUp<TenantBillingCycleDataModel, TenantBillingCycleDataModel.TenantBillingCycleCompositeId>
            tenantBillingCycleDataCleanUp(
                    EntityManager entityManager,
                    TenantBillingCycleRepository repository) {

        DataCleanUp<TenantBillingCycleDataModel, TenantBillingCycleDataModel.TenantBillingCycleCompositeId> cleanup =
                new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TenantBillingCycleDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
