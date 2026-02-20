/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

public interface CourseRepository extends TenantScopedRepository<CourseDataModel, CourseDataModel.CourseCompositeId> {
}
