/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.attendance.interfaceadapters;

import com.akademiaplus.attendance.AttendanceRecordDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

/**
 * Repository for tenant-scoped attendance record entities.
 */
public interface AttendanceRecordRepository
        extends TenantScopedRepository<AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> {
}
