/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles schedule creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into the entity ID field and the {@code course} FK relationship.
 */
@Service
@RequiredArgsConstructor
public class ScheduleCreationUseCase {
    public static final String MAP_NAME = "scheduleMap";
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";
    public static final String ERROR_COURSE_NOT_FOUND = "Course not found: ";

    private final ApplicationContext applicationContext;
    private final ScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new schedule from the given request.
     *
     * @param dto the schedule creation request
     * @return the persisted schedule mapped to a response DTO
     */
    @Transactional
    public ScheduleCreationResponseDTO create(ScheduleCreationRequestDTO dto) {
        ScheduleDataModel saved = scheduleRepository.saveAndFlush(transform(dto));
        return modelMapper.map(saved, ScheduleCreationResponseDTO.class);
    }

    /**
     * Maps a {@link ScheduleCreationRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into the entity ID and the course FK relationship.
     * The course association is resolved via repository lookup.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public ScheduleDataModel transform(ScheduleCreationRequestDTO dto) {
        final ScheduleDataModel model = applicationContext.getBean(ScheduleDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        CourseDataModel course = courseRepository.findById(
                        new CourseDataModel.CourseCompositeId(tenantId, dto.getCourseId()))
                .orElseThrow(() -> new IllegalArgumentException(ERROR_COURSE_NOT_FOUND + dto.getCourseId()));
        model.setCourse(course);

        return model;
    }
}
