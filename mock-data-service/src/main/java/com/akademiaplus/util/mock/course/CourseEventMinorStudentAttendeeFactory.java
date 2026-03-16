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
 * Factory for creating {@link CourseEventMinorStudentAttendeeRecord} instances with unique FK pairs.
 *
 * <p>Requires course event and minor student IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class CourseEventMinorStudentAttendeeFactory
        implements DataFactory<CourseEventMinorStudentAttendeeRecord> {

    static final String ERROR_COURSE_EVENT_IDS_NOT_SET =
            "availableCourseEventIds must be set before generating";
    static final String ERROR_MINOR_STUDENT_IDS_NOT_SET =
            "availableMinorStudentIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableCourseEventIds = List.of();

    @Setter
    private List<Long> availableMinorStudentIds = List.of();

    /**
     * Generates up to {@code count} unique course-event-to-minor-student attendee pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link CourseEventMinorStudentAttendeeRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<CourseEventMinorStudentAttendeeRecord> generate(int count) {
        if (availableCourseEventIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_EVENT_IDS_NOT_SET);
        }
        if (availableMinorStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_MINOR_STUDENT_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<CourseEventMinorStudentAttendeeRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long courseEventId = availableCourseEventIds.get(
                    attempts % availableCourseEventIds.size());
            Long minorStudentId = availableMinorStudentIds.get(
                    (attempts / availableCourseEventIds.size()) % availableMinorStudentIds.size());
            String key = courseEventId + PAIR_SEPARATOR + minorStudentId;
            if (seen.add(key)) {
                result.add(new CourseEventMinorStudentAttendeeRecord(courseEventId, minorStudentId));
            }
            attempts++;
        }
        return result;
    }
}
