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
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import com.akademiaplus.utilities.exceptions.EntityDeletionNotAllowedException;
import com.akademiaplus.utilities.EntityType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DeleteEmployeeUseCase {
    private final EmployeeRepository employeeRepository;
    private final TenantContextHolder tenantContextHolder;

    public DeleteEmployeeUseCase(EmployeeRepository employeeRepository,
                                  TenantContextHolder tenantContextHolder) {
        this.employeeRepository = employeeRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public void delete(Long employeeId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        Optional<EmployeeDataModel> found = employeeRepository.findById(
                new EmployeeDataModel.EmployeeCompositeId(tenantId, employeeId));
        if(found.isPresent()) {
            try {
                employeeRepository.delete(found.get());
            } catch(DataIntegrityViolationException ex) {
                throw new EntityDeletionNotAllowedException(EntityType.EMPLOYEE, String.valueOf(employeeId), ex);
            }
        } else {
            throw new EntityNotFoundException(EntityType.EMPLOYEE, String.valueOf(employeeId));
        }
    }
}
