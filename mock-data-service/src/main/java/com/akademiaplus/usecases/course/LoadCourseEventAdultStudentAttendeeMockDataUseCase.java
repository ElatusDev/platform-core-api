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
import com.akademiaplus.util.mock.course.CourseEventAdultStudentAttendeeRecord;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Loads mock course-event-to-adult-student attendee bridge records into the database.
 */
@Service
public class LoadCourseEventAdultStudentAttendeeMockDataUseCase
        extends AbstractBridgeMockDataUseCase<CourseEventAdultStudentAttendeeRecord> {

    /**
     * Creates a new use case with the required data loader and cleanup.
     *
     * @param dataLoader  the native bridge data loader for course-event adult-student attendee records
     * @param dataCleanUp the native bridge data cleanup for the course_event_adult_student_attendees table
     */
    public LoadCourseEventAdultStudentAttendeeMockDataUseCase(
            NativeBridgeDataLoader<CourseEventAdultStudentAttendeeRecord> dataLoader,
            @Qualifier("courseEventAdultStudentAttendeeDataCleanUp") NativeBridgeDataCleanUp dataCleanUp) {
        super(dataLoader, dataCleanUp);
    }
}
