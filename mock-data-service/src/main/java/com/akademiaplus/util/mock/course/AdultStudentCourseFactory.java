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
 * Factory for creating {@link AdultStudentCourseRecord} instances with unique FK pairs.
 *
 * <p>Requires adult student and course IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class AdultStudentCourseFactory implements DataFactory<AdultStudentCourseRecord> {

    static final String ERROR_ADULT_STUDENT_IDS_NOT_SET =
            "availableAdultStudentIds must be set before generating";
    static final String ERROR_COURSE_IDS_NOT_SET =
            "availableCourseIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableAdultStudentIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    /**
     * Generates up to {@code count} unique adult-student-to-course pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link AdultStudentCourseRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<AdultStudentCourseRecord> generate(int count) {
        if (availableAdultStudentIds.isEmpty()) {
            throw new IllegalStateException(ERROR_ADULT_STUDENT_IDS_NOT_SET);
        }
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<AdultStudentCourseRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long adultStudentId = availableAdultStudentIds.get(
                    attempts % availableAdultStudentIds.size());
            Long courseId = availableCourseIds.get(
                    (attempts / availableAdultStudentIds.size()) % availableCourseIds.size());
            String key = adultStudentId + PAIR_SEPARATOR + courseId;
            if (seen.add(key)) {
                result.add(new AdultStudentCourseRecord(adultStudentId, courseId));
            }
            attempts++;
        }
        return result;
    }
}
