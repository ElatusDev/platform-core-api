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
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles course creation by transforming the OpenAPI request DTO
 * into the persistence data model.
 * <p>
 * Uses a named TypeMap ({@value MAP_NAME}) and prototype-scoped beans
 * via {@link ApplicationContext} to prevent ModelMapper deep-matching
 * pollution into nested JPA relationships and ID fields.
 * <p>
 * Validates that referenced collaborators and schedules exist before
 * persisting. Schedules are saved separately after the course is
 * persisted, since the schedule → course FK is read-only on the
 * JPA side.
 */
@Service
@RequiredArgsConstructor
public class CreateCourseUseCase {
    public static final String MAP_NAME = "courseMap";

    private final ApplicationContext applicationContext;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final CourseValidator courseValidator;
    private final ModelMapper modelMapper;

    /**
     * Creates and persists a new course from the given request.
     * <p>
     * Validates collaborator and schedule references, transforms the DTO,
     * saves the course and its schedule associations, and returns the
     * response DTO with the generated course ID.
     *
     * @param dto the course creation request
     * @return the persisted course mapped to a response DTO
     */
    @Transactional
    public CourseCreationResponseDTO create(CourseCreationRequestDTO dto) {
        CourseDataModel savedCourse = courseRepository.saveAndFlush(transform(dto));
        scheduleRepository.saveAll(
                courseValidator.validateSchedulesAvailable(dto.getTimeTableIds())
        );
        return modelMapper.map(savedCourse, CourseCreationResponseDTO.class);
    }

    /**
     * Maps a {@link CourseCreationRequestDTO} to a persistence-ready data model.
     * <p>
     * Uses a named TypeMap to prevent deep-matching of DTO fields
     * into nested JPA relationships. Collaborators are resolved via
     * {@link CourseValidator} and wired manually.
     *
     * @param dto the creation request
     * @return populated data model ready for persistence
     */
    public CourseDataModel transform(CourseCreationRequestDTO dto) {
        List<CollaboratorDataModel> existingCollaborators =
                courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds());

        final CourseDataModel model = applicationContext.getBean(CourseDataModel.class);
        modelMapper.map(dto, model, MAP_NAME);
        model.setAvailableCollaborators(existingCollaborators);
        return model;
    }
}
