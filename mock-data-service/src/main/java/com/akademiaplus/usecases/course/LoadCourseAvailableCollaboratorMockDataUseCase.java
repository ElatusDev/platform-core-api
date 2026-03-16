/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.course;

import com.akademiaplus.util.base.AbstractBridgeMockDataUseCase;
import com.akademiaplus.util.base.NativeBridgeDataCleanUp;
import com.akademiaplus.util.base.NativeBridgeDataLoader;
import com.akademiaplus.util.mock.course.CourseAvailableCollaboratorRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock course-available-collaborator bridge records into the database.
 */
@Service
public class LoadCourseAvailableCollaboratorMockDataUseCase
        extends AbstractBridgeMockDataUseCase<CourseAvailableCollaboratorRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for course-collaborator records
     * @param dataCleanUp the native bridge data cleanup for the course_available_collaborators table
     */
    public LoadCourseAvailableCollaboratorMockDataUseCase(
            NativeBridgeDataLoader<CourseAvailableCollaboratorRecord> dataLoader,
            @Qualifier("courseAvailableCollaboratorDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
