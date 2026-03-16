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
import com.akademiaplus.util.mock.course.CourseEventMinorStudentAttendeeRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock course-event-to-minor-student attendee bridge records into the database.
 */
@Service
public class LoadCourseEventMinorStudentAttendeeMockDataUseCase
        extends AbstractBridgeMockDataUseCase<CourseEventMinorStudentAttendeeRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for course-event minor-student attendee records
     * @param dataCleanUp the native bridge data cleanup for the course_event_minor_student_attendees table
     */
    public LoadCourseEventMinorStudentAttendeeMockDataUseCase(
            NativeBridgeDataLoader<CourseEventMinorStudentAttendeeRecord> dataLoader,
            @Qualifier("courseEventMinorStudentAttendeeDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
