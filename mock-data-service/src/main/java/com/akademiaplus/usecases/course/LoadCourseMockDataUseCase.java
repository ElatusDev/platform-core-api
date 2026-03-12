/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.course;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.util.base.AbstractMockDataUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataLoader;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock course records into the database.
 *
 * <p>No {@code CourseCreationUseCase} exists in the domain layer, so the
 * {@link com.akademiaplus.config.CourseDataLoaderConfiguration} provides
 * a direct entity-mapping transformer for the {@link DataLoader}.</p>
 */
@Service
public class LoadCourseMockDataUseCase
        extends AbstractMockDataUseCase<CourseCreationRequestDTO, CourseDataModel, CourseDataModel.CourseCompositeId> {

    public LoadCourseMockDataUseCase(
            DataLoader<CourseCreationRequestDTO, CourseDataModel, CourseDataModel.CourseCompositeId> dataLoader,
            @Qualifier("courseDataCleanUp")
            DataCleanUp<CourseDataModel, CourseDataModel.CourseCompositeId> dataCleanup) {
        super(dataLoader, dataCleanup);
    }
}
