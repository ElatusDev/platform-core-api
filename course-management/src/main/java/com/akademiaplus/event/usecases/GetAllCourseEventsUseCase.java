/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all course events in the current tenant.
 */
@Service
public class GetAllCourseEventsUseCase {

    private final CourseEventRepository courseEventRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllCourseEventsUseCase with the required dependencies.
     *
     * @param courseEventRepository the repository for course event data access
     * @param modelMapper          the mapper for entity-to-DTO conversion
     */
    public GetAllCourseEventsUseCase(CourseEventRepository courseEventRepository, ModelMapper modelMapper) {
        this.courseEventRepository = courseEventRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all course events for the current tenant context.
     *
     * @return a list of course event response DTOs
     */
    public List<GetCourseEventResponseDTO> getAll() {
        return courseEventRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetCourseEventResponseDTO.class))
                .toList();
    }
}
