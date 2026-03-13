/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.domain;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.program.domain.exception.ScheduleConflictException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates business rules for the Course entity.
 *
 * <p>Validates course invariants (schedule conflicts, capacity) and
 * produces DTOs for state-changing operations. Has zero I/O — all data
 * is received via {@link #get(CourseDataModel)}.</p>
 *
 * @see CourseDataModel
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class DomainCourse {

    private CourseDataModel dataModel;

    /**
     * Entry point — sets the data model for subsequent operations.
     *
     * @param course the course data model to operate on
     * @return this instance for fluent chaining
     */
    public DomainCourse get(CourseDataModel course) {
        this.dataModel = course;
        return this;
    }

    /**
     * Validates that no schedule in the list is assigned to another course.
     *
     * <p>Schedules already assigned to the given {@code courseId} are considered
     * valid — only schedules belonging to a <em>different</em> course trigger
     * the conflict exception.</p>
     *
     * @param schedules the schedules to check
     * @param courseId  the current course's ID (self-assignment is OK)
     * @return this instance for fluent chaining
     * @throws ScheduleConflictException if any schedule belongs to a different course
     */
    public DomainCourse validateScheduleConflict(List<ScheduleDataModel> schedules,
                                                  Long courseId) {
        List<ScheduleDataModel> conflicting = schedules.stream()
                .filter(s -> s.getCourseId() != null && !s.getCourseId().equals(courseId))
                .toList();
        if (!conflicting.isEmpty()) {
            String conflictInfo = conflicting.stream()
                    .map(s -> String.valueOf(s.getScheduleId()))
                    .collect(Collectors.joining(", "));
            throw new ScheduleConflictException(conflictInfo);
        }
        return this;
    }
}
