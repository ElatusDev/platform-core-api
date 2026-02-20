/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link CourseEventCreateRequestDTO} instances with fake data.
 *
 * <p>Requires course, schedule, and collaborator IDs to be injected via setters
 * before {@link #generate(int)} is called. The orchestrator wires these IDs
 * through post-load hooks after the respective entities are persisted.</p>
 */
@Component
@RequiredArgsConstructor
public class CourseEventFactory implements DataFactory<CourseEventCreateRequestDTO> {

    private final CourseEventDataGenerator generator;

    @Setter
    private List<Long> availableCourseIds = List.of();

    @Setter
    private List<Long> availableScheduleIds = List.of();

    @Setter
    private List<Long> availableCollaboratorIds = List.of();

    @Override
    public List<CourseEventCreateRequestDTO> generate(int count) {
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException("availableCourseIds must be set before generating course events");
        }
        if (availableScheduleIds.isEmpty()) {
            throw new IllegalStateException("availableScheduleIds must be set before generating course events");
        }
        if (availableCollaboratorIds.isEmpty()) {
            throw new IllegalStateException("availableCollaboratorIds must be set before generating course events");
        }
        List<CourseEventCreateRequestDTO> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long courseId = availableCourseIds.get(i % availableCourseIds.size());
            Long scheduleId = availableScheduleIds.get(i % availableScheduleIds.size());
            Long collaboratorId = availableCollaboratorIds.get(i % availableCollaboratorIds.size());
            events.add(createCourseEvent(courseId, scheduleId, collaboratorId));
        }
        return events;
    }

    private CourseEventCreateRequestDTO createCourseEvent(Long courseId, Long scheduleId, Long collaboratorId) {
        CourseEventCreateRequestDTO dto = new CourseEventCreateRequestDTO();
        dto.setDate(generator.eventDate());
        dto.setTitle(generator.eventTitle());
        dto.setDescription(generator.eventDescription());
        dto.setCourseId(courseId);
        dto.setScheduleId(scheduleId);
        dto.setInstructorId(collaboratorId);
        return dto;
    }
}
