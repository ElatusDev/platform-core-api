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
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.event.usecases.CourseEventCreationUseCase;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.program.usecases.ScheduleCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for course-related mock data loader and cleanup beans.
 *
 * <p>Course has no {@code CourseCreationUseCase} in the domain layer, so the
 * transformer maps the DTO fields directly onto the prototype-scoped entity bean
 * obtained from the {@link ApplicationContext}.</p>
 */
@Configuration
public class CourseDataLoaderConfiguration {

    // ── Course (no domain creation use case — direct mapping) ──

    @Bean
    public DataLoader<CourseCreationRequestDTO, CourseDataModel, Long> courseDataLoader(
            CourseRepository repository,
            DataFactory<CourseCreationRequestDTO> courseFactory,
            ApplicationContext applicationContext) {

        return new DataLoader<>(repository, dto -> {
            CourseDataModel model = applicationContext.getBean(CourseDataModel.class);
            model.setCourseName(dto.getName());
            model.setCourseDescription(dto.getDescription());
            model.setMaxCapacity(dto.getMaxCapacity());
            return model;
        }, courseFactory);
    }

    @Bean
    public DataCleanUp<CourseDataModel, Long> courseDataCleanUp(
            EntityManager entityManager,
            CourseRepository repository) {

        DataCleanUp<CourseDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CourseDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Schedule ──

    @Bean
    public DataLoader<ScheduleCreationRequestDTO, ScheduleDataModel, Long> scheduleDataLoader(
            ScheduleRepository repository,
            DataFactory<ScheduleCreationRequestDTO> scheduleFactory,
            ScheduleCreationUseCase scheduleCreationUseCase) {

        return new DataLoader<>(repository, scheduleCreationUseCase::transform, scheduleFactory);
    }

    @Bean
    public DataCleanUp<ScheduleDataModel, Long> scheduleDataCleanUp(
            EntityManager entityManager,
            ScheduleRepository repository) {

        DataCleanUp<ScheduleDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(ScheduleDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── CourseEvent ──

    @Bean
    public DataLoader<CourseEventCreateRequestDTO, CourseEventDataModel, Long> courseEventDataLoader(
            CourseEventRepository repository,
            DataFactory<CourseEventCreateRequestDTO> courseEventFactory,
            CourseEventCreationUseCase courseEventCreationUseCase) {

        return new DataLoader<>(repository, courseEventCreationUseCase::transform, courseEventFactory);
    }

    @Bean
    public DataCleanUp<CourseEventDataModel, Long> courseEventDataCleanUp(
            EntityManager entityManager,
            CourseEventRepository repository) {

        DataCleanUp<CourseEventDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CourseEventDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
