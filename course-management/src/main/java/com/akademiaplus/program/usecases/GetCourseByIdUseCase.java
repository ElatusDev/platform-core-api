/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a course by its identifier within the current tenant.
 */
@Service
public class GetCourseByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final CourseRepository courseRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetCourseByIdUseCase with the required dependencies.
     *
     * @param courseRepository     the repository for course data access
     * @param tenantContextHolder the holder for the current tenant context
     * @param modelMapper         the mapper for entity-to-DTO conversion
     */
    public GetCourseByIdUseCase(CourseRepository courseRepository,
                                TenantContextHolder tenantContextHolder,
                                ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a course by its identifier within the current tenant context.
     *
     * @param courseId the unique identifier of the course
     * @return the course response DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no course is found with the given identifier
     */
    public GetCourseResponseDTO get(Long courseId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<CourseDataModel> queryResult = courseRepository.findById(
                new CourseDataModel.CourseCompositeId(tenantId, courseId));
        if (queryResult.isPresent()) {
            CourseDataModel found = queryResult.get();
            return modelMapper.map(found, GetCourseResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.COURSE, String.valueOf(courseId));
        }
    }
}
