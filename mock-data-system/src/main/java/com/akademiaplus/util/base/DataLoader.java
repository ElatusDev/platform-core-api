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
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component
@AllArgsConstructor
@Scope("prototype")
public class DataLoader<D, M, I> {
    private JpaRepository<@NonNull M, @NonNull I> repository;
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
