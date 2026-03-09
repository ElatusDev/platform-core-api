/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.attendance;

import com.akademiaplus.attendance.AttendanceSessionDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock attendance session records into the database.
 */
@Service
public class LoadAttendanceSessionMockDataUseCase
        extends AbstractMockDataUseCase<AttendanceSessionDataModel, AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> {

    /**
     * Constructs the use case with the session-specific data loader and cleanup.
     *
     * @param dataLoader  the data loader for attendance sessions
     * @param dataCleanup the data cleanup for attendance sessions
     */
    public LoadAttendanceSessionMockDataUseCase(
            DataLoader<AttendanceSessionDataModel, AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> dataLoader,
            @Qualifier("attendanceSessionDataCleanUp")
            DataCleanUp<AttendanceSessionDataModel, AttendanceSessionDataModel.AttendanceSessionCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
