/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link MembershipTutorDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteMembershipTutorUseCase {

    private final MembershipTutorRepository repository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteMembershipTutorUseCase(MembershipTutorRepository repository,
                                         TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link MembershipTutorDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param membershipTutorId the entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no entity exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long membershipTutorId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new MembershipTutorDataModel.MembershipTutorCompositeId(tenantId, membershipTutorId),
                EntityType.MEMBERSHIP_TUTOR,
                String.valueOf(membershipTutorId));
    }
}
