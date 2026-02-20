/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link PaymentTutorDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeletePaymentTutorUseCase {

    private final PaymentTutorRepository repository;
    private final TenantContextHolder tenantContextHolder;

    public DeletePaymentTutorUseCase(PaymentTutorRepository repository,
                                      TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link PaymentTutorDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param paymentTutorId the entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no entity exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long paymentTutorId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new PaymentTutorDataModel.PaymentTutorCompositeId(tenantId, paymentTutorId),
                EntityType.PAYMENT_TUTOR,
                String.valueOf(paymentTutorId));
    }
}
