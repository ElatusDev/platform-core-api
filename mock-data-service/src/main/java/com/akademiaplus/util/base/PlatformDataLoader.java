/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.base;

import com.akademiaplus.exceptions.FailToGenerateMockDataException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.function.Function;

/**
 * Data loader for platform-level entities that use {@link JpaRepository}
 * instead of {@code TenantScopedRepository}.
 *
 * <p>Platform-level entities (e.g., DemoRequest, PushDevice) are not
 * tenant-scoped and use standard auto-increment {@code Long} primary keys.</p>
 *
 * @param <D> the DTO or data model type produced by the factory
 * @param <M> the JPA entity type
 */
@Slf4j
@AllArgsConstructor
public class PlatformDataLoader<D, M> {

    private JpaRepository<M, Long> repository;
    private Function<D, M> transformer;
    private DataFactory<D> factory;

    /**
     * Generates {@code count} entities and persists them individually.
     *
     * @param count number of entities to generate
     */
    @Transactional
    public void load(int count) {
        try {
            List<M> models = factory.generate(count).stream().map(transformer).toList();
            models.forEach(e -> {
                repository.save(e);
                repository.flush();
            });
        } catch (Exception e) {
            throw new FailToGenerateMockDataException(e);
        }
    }
}
