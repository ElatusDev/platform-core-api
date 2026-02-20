/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.adultstudent.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import org.springframework.stereotype.Repository;

@Repository
public interface AdultStudentRepository extends TenantScopedRepository<AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> {
}
