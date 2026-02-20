/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.usecases;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.usecases.DeleteUseCaseSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles soft-deletion of an {@link EmployeeDataModel} by composite key.
 * <p>
 * Delegates to {@link DeleteUseCaseSupport#executeDelete} for the
 * find-or-404 → delete → catch-constraint-409 pattern.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class DeleteEmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteEmployeeUseCase(EmployeeRepository employeeRepository,
                                 TenantContextHolder tenantContextHolder) {
        this.employeeRepository = employeeRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Soft-deletes the {@link EmployeeDataModel} identified by the given ID
     * within the current tenant context.
     *
     * @param employeeId the employee's entity-specific ID
     * @throws com.akademiaplus.utilities.exceptions.EntityNotFoundException
     *         if no employee exists with the given composite key
     * @throws com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException
     *         if a database constraint prevents deletion
     * @throws com.akademiaplus.utilities.exceptions.InvalidTenantException
     *         if no tenant context is set
     */
    @Transactional
    public void delete(Long employeeId) {
        Long tenantId = tenantContextHolder.requireTenantId();
        DeleteUseCaseSupport.executeDelete(
                employeeRepository,
                new EmployeeDataModel.EmployeeCompositeId(tenantId, employeeId),
                EntityType.EMPLOYEE,
                String.valueOf(employeeId));
    }
}
