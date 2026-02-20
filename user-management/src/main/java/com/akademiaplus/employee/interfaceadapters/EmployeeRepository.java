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

@Repository
public interface EmployeeRepository extends TenantScopedRepository<EmployeeDataModel, EmployeeDataModel.EmployeeCompositeId> {
}
