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
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.program.usecases.ScheduleCreationUseCase;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import com.akademiaplus.util.base.NativeBridgeDataCleanUp;
import com.akademiaplus.util.base.NativeBridgeDataLoader;
import com.akademiaplus.util.mock.course.AdultStudentCourseFactory;
import com.akademiaplus.util.mock.course.AdultStudentCourseRecord;
import com.akademiaplus.util.mock.course.CourseAvailableCollaboratorFactory;
import com.akademiaplus.util.mock.course.CourseAvailableCollaboratorRecord;
import com.akademiaplus.util.mock.course.CourseEventAdultStudentAttendeeFactory;
import com.akademiaplus.util.mock.course.CourseEventAdultStudentAttendeeRecord;
import com.akademiaplus.util.mock.course.CourseEventMinorStudentAttendeeFactory;
import com.akademiaplus.util.mock.course.CourseEventMinorStudentAttendeeRecord;
import com.akademiaplus.util.mock.course.MinorStudentCourseFactory;
import com.akademiaplus.util.mock.course.MinorStudentCourseRecord;
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
    public DataLoader<CourseCreationRequestDTO, CourseDataModel, CourseDataModel.CourseCompositeId> courseDataLoader(
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
    public DataCleanUp<CourseDataModel, CourseDataModel.CourseCompositeId> courseDataCleanUp(
            EntityManager entityManager,
            CourseRepository repository) {

        DataCleanUp<CourseDataModel, CourseDataModel.CourseCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CourseDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Schedule ──

    @Bean
    public DataLoader<ScheduleCreationRequestDTO, ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> scheduleDataLoader(
            ScheduleRepository repository,
            DataFactory<ScheduleCreationRequestDTO> scheduleFactory,
            ScheduleCreationUseCase scheduleCreationUseCase) {

        return new DataLoader<>(repository, scheduleCreationUseCase::transform, scheduleFactory);
    }

    @Bean
    public DataCleanUp<ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> scheduleDataCleanUp(
            EntityManager entityManager,
            ScheduleRepository repository) {

        DataCleanUp<ScheduleDataModel, ScheduleDataModel.ScheduleCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(ScheduleDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── CourseEvent ──

    @Bean
    public DataLoader<CourseEventCreateRequestDTO, CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> courseEventDataLoader(
            CourseEventRepository repository,
            DataFactory<CourseEventCreateRequestDTO> courseEventFactory,
            CourseEventCreationUseCase courseEventCreationUseCase) {

        return new DataLoader<>(repository, courseEventCreationUseCase::transform, courseEventFactory);
    }

    @Bean
    public DataCleanUp<CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> courseEventDataCleanUp(
            EntityManager entityManager,
            CourseEventRepository repository) {

        DataCleanUp<CourseEventDataModel, CourseEventDataModel.CourseEventCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CourseEventDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── CourseAvailableCollaborator (bridge table) ──

    /**
     * Creates the native bridge data loader for course-available-collaborator records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the course-available-collaborator factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<CourseAvailableCollaboratorRecord> courseAvailableCollaboratorDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            CourseAvailableCollaboratorFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO course_available_collaborators (tenant_id, course_id, collaborator_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.courseId(), r.collaboratorId()});
    }

    /**
     * Creates the native bridge data cleanup for the course_available_collaborators table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp courseAvailableCollaboratorDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("course_available_collaborators");
        return cleanUp;
    }

    // ── AdultStudentCourse (bridge table) ──

    /**
     * Creates the native bridge data loader for adult-student-course records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the adult-student-course factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<AdultStudentCourseRecord> adultStudentCourseDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            AdultStudentCourseFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO adult_student_courses (tenant_id, adult_student_id, course_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.adultStudentId(), r.courseId()});
    }

    /**
     * Creates the native bridge data cleanup for the adult_student_courses table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp adultStudentCourseDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("adult_student_courses");
        return cleanUp;
    }

    // ── MinorStudentCourse (bridge table) ──

    /**
     * Creates the native bridge data loader for minor-student-course records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the minor-student-course factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<MinorStudentCourseRecord> minorStudentCourseDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            MinorStudentCourseFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO minor_student_courses (tenant_id, minor_student_id, course_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.minorStudentId(), r.courseId()});
    }

    /**
     * Creates the native bridge data cleanup for the minor_student_courses table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp minorStudentCourseDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("minor_student_courses");
        return cleanUp;
    }

    // ── CourseEventAdultStudentAttendee (bridge table) ──

    /**
     * Creates the native bridge data loader for course-event adult-student attendee records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the course-event adult-student attendee factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<CourseEventAdultStudentAttendeeRecord> courseEventAdultStudentAttendeeDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            CourseEventAdultStudentAttendeeFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO course_event_adult_student_attendees (tenant_id, course_event_id, adult_student_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.courseEventId(), r.adultStudentId()});
    }

    /**
     * Creates the native bridge data cleanup for the course_event_adult_student_attendees table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp courseEventAdultStudentAttendeeDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("course_event_adult_student_attendees");
        return cleanUp;
    }

    // ── CourseEventMinorStudentAttendee (bridge table) ──

    /**
     * Creates the native bridge data loader for course-event minor-student attendee records.
     *
     * @param entityManager       the JPA entity manager
     * @param tenantContextHolder the tenant context holder
     * @param factory             the course-event minor-student attendee factory
     * @return a configured native bridge data loader
     */
    @Bean
    public NativeBridgeDataLoader<CourseEventMinorStudentAttendeeRecord> courseEventMinorStudentAttendeeDataLoader(
            EntityManager entityManager,
            TenantContextHolder tenantContextHolder,
            CourseEventMinorStudentAttendeeFactory factory) {

        return new NativeBridgeDataLoader<>(
                entityManager, tenantContextHolder, factory,
                "INSERT INTO course_event_minor_student_attendees (tenant_id, course_event_id, minor_student_id) VALUES (?, ?, ?)",
                r -> new Object[]{r.courseEventId(), r.minorStudentId()});
    }

    /**
     * Creates the native bridge data cleanup for the course_event_minor_student_attendees table.
     *
     * @param entityManager the JPA entity manager
     * @return a configured native bridge data cleanup
     */
    @Bean
    public NativeBridgeDataCleanUp courseEventMinorStudentAttendeeDataCleanUp(EntityManager entityManager) {
        NativeBridgeDataCleanUp cleanUp = new NativeBridgeDataCleanUp(entityManager);
        cleanUp.setTableName("course_event_minor_student_attendees");
        return cleanUp;
    }
}
