/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles course event creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into nested JPA relationships (course, collaborator, schedule,
 * adultAttendees, minorAttendees) and the entity ID field.
 */
@Service
@RequiredArgsConstructor
public class CourseEventCreationUseCase {
    public static final String MAP_NAME = "courseEventMap";
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";
    public static final String ERROR_COURSE_NOT_FOUND = "Course not found: ";
    public static final String ERROR_COLLABORATOR_NOT_FOUND = "Collaborator not found: ";
    public static final String ERROR_SCHEDULE_NOT_FOUND = "Schedule not found: ";

    private final ApplicationContext applicationContext;
    private final CourseEventRepository courseEventRepository;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new course event from the given request.
     *
     * @param dto the course event creation request
     * @return the persisted course event mapped to a response DTO
     */
    @Transactional
    public CourseEventCreateResponseDTO create(CourseEventCreateRequestDTO dto) {
        CourseEventDataModel saved = courseEventRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, CourseEventCreateResponseDTO.class);
    }

    /**
     * Maps a {@link CourseEventCreateRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into nested JPA relationships. FK associations are resolved via
     * repository lookups, not through ModelMapper.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public CourseEventDataModel transform(CourseEventCreateRequestDTO dto) {
        final CourseEventDataModel model = applicationContext.getBean(CourseEventDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));

        CourseDataModel course = courseRepository.findById(
                        new CourseDataModel.CourseCompositeId(tenantId, dto.getCourseId()))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_COURSE_NOT_FOUND + dto.getCourseId()));
        model.setCourse(course);

        CollaboratorDataModel collaborator = collaboratorRepository.findById(
                        new CollaboratorDataModel.CollaboratorCompositeId(tenantId, dto.getInstructorId()))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_COLLABORATOR_NOT_FOUND + dto.getInstructorId()));
        model.setCollaborator(collaborator);

        ScheduleDataModel schedule = scheduleRepository.findById(
                        new ScheduleDataModel.ScheduleCompositeId(tenantId, dto.getScheduleId()))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_SCHEDULE_NOT_FOUND + dto.getScheduleId()));
        model.setSchedule(schedule);

        return model;
    }
}
