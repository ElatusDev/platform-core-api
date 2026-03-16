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
 * Factory for creating {@link CourseAvailableCollaboratorRecord} instances with unique FK pairs.
 *
 * <p>Requires course and collaborator IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class CourseAvailableCollaboratorFactory implements DataFactory<CourseAvailableCollaboratorRecord> {

    static final String ERROR_COURSE_IDS_NOT_SET =
            "availableCourseIds must be set before generating";
    static final String ERROR_COLLABORATOR_IDS_NOT_SET =
            "availableCollaboratorIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableCourseIds = List.of();

    @Setter
    private List<Long> availableCollaboratorIds = List.of();

    /**
     * Generates up to {@code count} unique course-collaborator pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link CourseAvailableCollaboratorRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<CourseAvailableCollaboratorRecord> generate(int count) {
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_IDS_NOT_SET);
        }
        if (availableCollaboratorIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COLLABORATOR_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<CourseAvailableCollaboratorRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long courseId = availableCourseIds.get(attempts % availableCourseIds.size());
            Long collaboratorId = availableCollaboratorIds.get(
                    (attempts / availableCourseIds.size()) % availableCollaboratorIds.size());
            String key = courseId + PAIR_SEPARATOR + collaboratorId;
            if (seen.add(key)) {
                result.add(new CourseAvailableCollaboratorRecord(courseId, collaboratorId));
            }
            attempts++;
        }
        return result;
    }
}
