/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of an {@link AdultStudentDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteAdultStudentUseCase {

    private final AdultStudentRepository adultStudentRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteAdultStudentUseCase(AdultStudentRepository adultStudentRepository,
                                     TenantContextHolder tenantContextHolder) {
        this.adultStudentRepository = adultStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link AdultStudentDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param adultStudentId the adult student's entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no adult student exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long adultStudentId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                adultStudentRepository,
                new AdultStudentDataModel.AdultStudentCompositeId(tenantId, adultStudentId),
                EntityType.ADULT_STUDENT,
                String.valueOf(adultStudentId));
    }
}
