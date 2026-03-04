/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.usecases.CourseEventCreationUseCase;
import com.akademiaplus.event.usecases.CourseEventUpdateUseCase;
import com.akademiaplus.program.usecases.CourseUpdateUseCase;
import com.akademiaplus.program.usecases.CreateCourseUseCase;
import com.akademiaplus.program.usecases.ScheduleCreationUseCase;
import com.akademiaplus.program.usecases.ScheduleUpdateUseCase;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleUpdateRequestDTO;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for course-management DTO → DataModel conversions.
 * <p>
 * Prevents ModelMapper from deep-matching DTO fields into nested JPA
 * relationships and entity ID fields.
 *
 * @see CreateCourseUseCase
 * @see ScheduleCreationUseCase
 * @see CourseEventCreationUseCase
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
        registerCourseUpdateMap();
        registerScheduleMap();
        registerScheduleUpdateMap();
        registerCourseEventMap();
        registerCourseEventUpdateMap();

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

    private void registerCourseUpdateMap() {
        modelMapper.createTypeMap(
                CourseUpdateRequestDTO.class,
                CourseDataModel.class,
                CourseUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CourseDataModel::setCourseId);
            mapper.skip(CourseDataModel::setSchedules);
            mapper.skip(CourseDataModel::setAvailableCollaborators);
        }).implicitMappings();
    }

    private void registerScheduleMap() {
        modelMapper.createTypeMap(
                ScheduleCreationRequestDTO.class,
                ScheduleDataModel.class,
                ScheduleCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(ScheduleDataModel::setScheduleId);
            mapper.skip(ScheduleDataModel::setCourse);
        }).implicitMappings();
    }

    private void registerScheduleUpdateMap() {
        modelMapper.createTypeMap(
                ScheduleUpdateRequestDTO.class,
                ScheduleDataModel.class,
                ScheduleUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(ScheduleDataModel::setScheduleId);
            mapper.skip(ScheduleDataModel::setCourse);
        }).implicitMappings();
    }

    private void registerCourseEventMap() {
        modelMapper.createTypeMap(
                CourseEventCreateRequestDTO.class,
                CourseEventDataModel.class,
                CourseEventCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CourseEventDataModel::setCourseEventId);
            mapper.skip(CourseEventDataModel::setCourse);
            mapper.skip(CourseEventDataModel::setCollaborator);
            mapper.skip(CourseEventDataModel::setSchedule);
            mapper.skip(CourseEventDataModel::setAdultAttendees);
            mapper.skip(CourseEventDataModel::setMinorAttendees);
            mapper.map(CourseEventCreateRequestDTO::getDate, CourseEventDataModel::setEventDate);
            mapper.map(CourseEventCreateRequestDTO::getTitle, CourseEventDataModel::setEventTitle);
            mapper.map(CourseEventCreateRequestDTO::getDescription, CourseEventDataModel::setEventDescription);
        }).implicitMappings();
    }

    private void registerCourseEventUpdateMap() {
        modelMapper.createTypeMap(
                CourseEventUpdateRequestDTO.class,
                CourseEventDataModel.class,
                CourseEventUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CourseEventDataModel::setCourseEventId);
            mapper.skip(CourseEventDataModel::setCourse);
            mapper.skip(CourseEventDataModel::setCollaborator);
            mapper.skip(CourseEventDataModel::setSchedule);
            mapper.skip(CourseEventDataModel::setAdultAttendees);
            mapper.skip(CourseEventDataModel::setMinorAttendees);
            mapper.map(CourseEventUpdateRequestDTO::getTitle, CourseEventDataModel::setEventTitle);
            mapper.map(CourseEventUpdateRequestDTO::getDescription, CourseEventDataModel::setEventDescription);
        }).implicitMappings();
    }
}
