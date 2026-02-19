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

    private final ApplicationContext applicationContext;
    private final ScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new schedule from the given request.
     *
     * @param dto the schedule creation request
     * @return the persisted schedule mapped to a response DTO
     */
    @Transactional
    public ScheduleCreationResponseDTO create(ScheduleCreationRequestDTO dto) {
        ScheduleDataModel saved = scheduleRepository.save(transform(dto));
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

        CourseDataModel course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        model.setCourse(course);

        return model;
    }
}
