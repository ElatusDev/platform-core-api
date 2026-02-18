/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.usecases.TenantCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequence;
import com.akademiaplus.utilities.idgeneration.interfaceadapters.TenantSequenceRepository;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantDataLoaderConfiguration {

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
}
