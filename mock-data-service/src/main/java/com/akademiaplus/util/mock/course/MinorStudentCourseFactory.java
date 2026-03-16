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
 * Factory for creating {@link MinorStudentCourseRecord} instances with unique FK pairs.
 *
 * <p>Requires minor student and course IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class MinorStudentCourseFactory implements DataFactory<MinorStudentCourseRecord> {

    static final String ERROR_MINOR_STUDENT_IDS_NOT_SET =
            "availableMinorStudentIds must be set before generating";
    static final String ERROR_COURSE_IDS_NOT_SET =
            "availableCourseIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableMinorStudentIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    /**
     * Generates up to {@code count} unique minor-student-to-course pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link MinorStudentCourseRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<MinorStudentCourseRecord> generate(int count) {
        if (availableMinorStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_MINOR_STUDENT_IDS_NOT_SET);
        }
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<MinorStudentCourseRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long minorStudentId = availableMinorStudentIds.get(
                    attempts % availableMinorStudentIds.size());
            Long courseId = availableCourseIds.get(
                    (attempts / availableMinorStudentIds.size()) % availableCourseIds.size());
            String key = minorStudentId + PAIR_SEPARATOR + courseId;
            if (seen.add(key)) {
                result.add(new MinorStudentCourseRecord(minorStudentId, courseId));
            }
            attempts++;
        }
        return result;
    }
}
