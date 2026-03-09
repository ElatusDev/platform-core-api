/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.attendance;

import com.akademiaplus.attendance.AttendanceRecordDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock attendance record entries into the database.
 */
@Service
public class LoadAttendanceRecordMockDataUseCase
        extends AbstractMockDataUseCase<AttendanceRecordDataModel, AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> {

    /**
     * Constructs the use case with the record-specific data loader and cleanup.
     *
     * @param dataLoader  the data loader for attendance records
     * @param dataCleanup the data cleanup for attendance records
     */
    public LoadAttendanceRecordMockDataUseCase(
            DataLoader<AttendanceRecordDataModel, AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> dataLoader,
            @Qualifier("attendanceRecordDataCleanUp")
            DataCleanUp<AttendanceRecordDataModel, AttendanceRecordDataModel.AttendanceRecordCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
