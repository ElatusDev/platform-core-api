/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.program.interfaceadapters.CourseRepository;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all courses in the current tenant.
 */
@Service
public class GetAllCoursesUseCase {

    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllCoursesUseCase with the required dependencies.
     *
     * @param courseRepository the repository for course data access
     * @param modelMapper     the mapper for entity-to-DTO conversion
     */
    public GetAllCoursesUseCase(CourseRepository courseRepository, ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all courses for the current tenant context.
     *
     * @return a list of course response DTOs
     */
    public List<GetCourseResponseDTO> getAll() {
        return courseRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetCourseResponseDTO.class))
                .toList();
    }
}
