/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.course;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock schedule records into the database.
 */
@Service
public class LoadScheduleMockDataUseCase
        extends AbstractMockDataUseCase<ScheduleCreationRequestDTO, ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> {

    public LoadScheduleMockDataUseCase(
            DataLoader<ScheduleCreationRequestDTO, ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> dataLoader,
            @Qualifier("scheduleDataCleanUp")
            DataCleanUp<ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
