/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of a {@link PaymentAdultStudentDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeletePaymentAdultStudentUseCase {

    private final PaymentAdultStudentRepository repository;
    private final TenantContextHolder tenantContextHolder;

    public DeletePaymentAdultStudentUseCase(PaymentAdultStudentRepository repository,
                                             TenantContextHolder tenantContextHolder) {
        this.repository = repository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link PaymentAdultStudentDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param paymentAdultStudentId the entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no entity exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long paymentAdultStudentId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                repository,
                new PaymentAdultStudentDataModel.PaymentAdultStudentCompositeId(tenantId, paymentAdultStudentId),
                EntityType.PAYMENT_ADULT_STUDENT,
                String.valueOf(paymentAdultStudentId));
    }
}
