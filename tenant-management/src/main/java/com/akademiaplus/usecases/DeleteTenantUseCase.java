/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.interfaceadapters.TenantRepository;
import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link TenantDataModel} by its single ID.
 * <p>
 * Unlike other entities that use composite keys (tenantId + entityId),
 * {@code TenantDataModel} uses a single {@code @Id} ({@code tenantId})
 * because it IS the tenant root entity. The ID is passed directly to
 * {@link DeleteUseCaseSupport#executeDelete}.
 * <p>
 * Note: cascading soft-delete of all child entities is out of scope
 * and should be addressed in a future ADR.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteTenantUseCase {

    private final TenantRepository repository;

    public DeleteTenantUseCase(TenantRepository repository) {
        this.repository = repository;
    }

    /**
     * Soft-deletes the {@link TenantDataModel} identified by the given tenant ID.
     *
     * @param tenantId the tenant ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no tenant exists with the given ID
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     */
    @Transactional
    public void delete(Long tenantId) {
        DeleteUseCaseSupport.executeDelete(
                repository,
                tenantId,
                EntityType.TENANT,
                String.valueOf(tenantId));
    }
}
