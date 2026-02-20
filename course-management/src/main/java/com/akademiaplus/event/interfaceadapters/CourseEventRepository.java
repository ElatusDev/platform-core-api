/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.interfaceadapters;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseEventRepository extends TenantScopedRepository<CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> {
}
