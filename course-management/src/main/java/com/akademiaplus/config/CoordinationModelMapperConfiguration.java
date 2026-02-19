/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.program.usecases.CreateCourseUseCase;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for course-management DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into nested JPA
 * relationships ({@code schedules}, {@code availableCollaborators}) and
 * the entity ID field ({@code courseId}).
 *
 * @see CreateCourseUseCase
 */
@Configuration
public class CoordinationModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public CoordinationModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerCourseMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerCourseMap() {
        modelMapper.createTypeMap(
                CourseCreationRequestDTO.class,
                CourseDataModel.class,
                CreateCourseUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CourseDataModel::setCourseId);
            mapper.skip(CourseDataModel::setSchedules);
            mapper.skip(CourseDataModel::setAvailableCollaborators);
        }).implicitMappings();
    }
}
