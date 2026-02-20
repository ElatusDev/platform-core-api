/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.usecases;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link MinorStudentDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteMinorStudentUseCase {

    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteMinorStudentUseCase(MinorStudentRepository minorStudentRepository,
                                     TenantContextHolder tenantContextHolder) {
        this.minorStudentRepository = minorStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link MinorStudentDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param minorStudentId the minor student's entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no minor student exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long minorStudentId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                minorStudentRepository,
                new MinorStudentDataModel.MinorStudentCompositeId(tenantId, minorStudentId),
                EntityType.MINOR_STUDENT,
                String.valueOf(minorStudentId));
    }
}
