/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.employee.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.employee.EmployeeDataModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends TenantScopedRepository<EmployeeDataModel, EmployeeDataModel.EmployeeCompositeId> {

    /**
     * Finds an employee by their internal authentication ID within the current tenant.
     *
     * @param internalAuthId the internal auth ID from the JWT user_id claim
     * @return an {@link Optional} containing the employee, or empty if not found
     */
    Optional<EmployeeDataModel> findByInternalAuthId(Long internalAuthId);
}
