/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.customer.minorstudent.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import org.springframework.stereotype.Repository;

@Repository
public interface MinorStudentRepository extends TenantScopedRepository<MinorStudentDataModel, MinorStudentDataModel.MinorStudentCompositeId> {
}
