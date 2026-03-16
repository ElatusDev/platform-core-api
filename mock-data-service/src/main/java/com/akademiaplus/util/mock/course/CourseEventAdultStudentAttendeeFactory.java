/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for creating {@link CourseEventAdultStudentAttendeeRecord} instances with unique FK pairs.
 *
 * <p>Requires course event and adult student IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class CourseEventAdultStudentAttendeeFactory
        implements DataFactory<CourseEventAdultStudentAttendeeRecord> {

    static final String ERROR_COURSE_EVENT_IDS_NOT_SET =
            "availableCourseEventIds must be set before generating";
    static final String ERROR_ADULT_STUDENT_IDS_NOT_SET =
            "availableAdultStudentIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableCourseEventIds = List.of();

    @Setter
    private List<Long> availableAdultStudentIds = List.of();

    /**
     * Generates up to {@code count} unique course-event-to-adult-student attendee pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link CourseEventAdultStudentAttendeeRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<CourseEventAdultStudentAttendeeRecord> generate(int count) {
        if (availableCourseEventIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_EVENT_IDS_NOT_SET);
        }
        if (availableAdultStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_ADULT_STUDENT_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<CourseEventAdultStudentAttendeeRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long courseEventId = availableCourseEventIds.get(
                    attempts % availableCourseEventIds.size());
            Long adultStudentId = availableAdultStudentIds.get(
                    (attempts / availableCourseEventIds.size()) % availableAdultStudentIds.size());
            String key = courseEventId + PAIR_SEPARATOR + adultStudentId;
            if (seen.add(key)) {
                result.add(new CourseEventAdultStudentAttendeeRecord(courseEventId, adultStudentId));
            }
            attempts++;
        }
        return result;
    }
}
