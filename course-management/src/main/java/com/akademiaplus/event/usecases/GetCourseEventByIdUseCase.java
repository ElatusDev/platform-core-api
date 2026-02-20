/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a course event by its identifier within the current tenant.
 */
@Service
public class GetCourseEventByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final CourseEventRepository courseEventRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetCourseEventByIdUseCase with the required dependencies.
     *
     * @param courseEventRepository the repository for course event data access
     * @param tenantContextHolder  the holder for the current tenant context
     * @param modelMapper          the mapper for entity-to-DTO conversion
     */
    public GetCourseEventByIdUseCase(CourseEventRepository courseEventRepository,
                                     TenantContextHolder tenantContextHolder,
                                     ModelMapper modelMapper) {
        this.courseEventRepository = courseEventRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a course event by its identifier within the current tenant context.
     *
     * @param courseEventId the unique identifier of the course event
     * @return the course event response DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no course event is found with the given identifier
     */
    public GetCourseEventResponseDTO get(Long courseEventId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<CourseEventDataModel> queryResult = courseEventRepository.findById(
                new CourseEventDataModel.CourseEventCompositeId(tenantId, courseEventId));
        if (queryResult.isPresent()) {
            CourseEventDataModel found = queryResult.get();
            return modelMapper.map(found, GetCourseEventResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.COURSE_EVENT, String.valueOf(courseEventId));
        }
    }
}
