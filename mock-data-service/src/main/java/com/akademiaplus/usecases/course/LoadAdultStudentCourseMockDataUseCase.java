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
import com.akademiaplus.util.mock.course.AdultStudentCourseRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock adult-student-to-course bridge records into the database.
 */
@Service
public class LoadAdultStudentCourseMockDataUseCase
        extends AbstractBridgeMockDataUseCase<AdultStudentCourseRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for adult-student-course records
     * @param dataCleanUp the native bridge data cleanup for the adult_student_courses table
     */
    public LoadAdultStudentCourseMockDataUseCase(
            NativeBridgeDataLoader<AdultStudentCourseRecord> dataLoader,
            @Qualifier("adultStudentCourseDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
