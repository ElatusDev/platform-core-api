/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.FailToGenerateMockDataException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

import java.util.List;
import java.util.function.Function;

/**
 * Generic data loader that generates mock entities and persists them via a JPA repository.
 *
 * <p>Instances are created as {@code @Bean} definitions in the DataLoaderConfiguration classes,
 * not through component scanning, to allow Spring to resolve the correct repository by type.</p>
 */
@Slf4j
@AllArgsConstructor
public class DataLoader<D, M, I> {
    private TenantScopedRepository<@NonNull M, @NonNull I> repository;
    private Function<D, M> transformer;
    private DataFactory<D> factory;

    @Transactional
    public void load(int count) {
        try {
            List<M> employeeDataModels = factory.generate(count).stream().map(transformer).toList();
            employeeDataModels.forEach( e -> {
                repository.save(e);
                repository.flush();
            });
        } catch (Exception e) {
            throw new FailToGenerateMockDataException(e);
        }
    }
}
