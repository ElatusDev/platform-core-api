/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.course;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock course event records into the database.
 */
@Service
public class LoadCourseEventMockDataUseCase
        extends AbstractMockDataUseCase<CourseEventCreateRequestDTO, CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> {

    public LoadCourseEventMockDataUseCase(
            DataLoader<CourseEventCreateRequestDTO, CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> dataLoader,
            @Qualifier("courseEventDataCleanUp")
            DataCleanUp<CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
