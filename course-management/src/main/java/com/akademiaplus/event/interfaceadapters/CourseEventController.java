/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.interfaceadapters;

import com.akademiaplus.event.usecases.CourseEventCreationUseCase;
import com.akademiaplus.event.usecases.DeleteCourseEventUseCase;
import com.akademiaplus.event.usecases.GetAllCourseEventsUseCase;
import com.akademiaplus.event.usecases.GetCourseEventByIdUseCase;
import openapi.akademiaplus.domain.course.management.api.CourseEventsApi;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateResponseDTO;
import openapi.akademiaplus.domain.course.management.dto.GetCourseEventResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for course event management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/course-management")
public class CourseEventController implements CourseEventsApi {

    private final CourseEventCreationUseCase courseEventCreationUseCase;
    private final DeleteCourseEventUseCase deleteCourseEventUseCase;
    private final GetAllCourseEventsUseCase getAllCourseEventsUseCase;
    private final GetCourseEventByIdUseCase getCourseEventByIdUseCase;

    public CourseEventController(CourseEventCreationUseCase courseEventCreationUseCase,
                                 DeleteCourseEventUseCase deleteCourseEventUseCase,
                                 GetAllCourseEventsUseCase getAllCourseEventsUseCase,
                                 GetCourseEventByIdUseCase getCourseEventByIdUseCase) {
        this.courseEventCreationUseCase = courseEventCreationUseCase;
        this.deleteCourseEventUseCase = deleteCourseEventUseCase;
        this.getAllCourseEventsUseCase = getAllCourseEventsUseCase;
        this.getCourseEventByIdUseCase = getCourseEventByIdUseCase;
    }

    @Override
    public ResponseEntity<CourseEventCreateResponseDTO> createCourseEvent(
            CourseEventCreateRequestDTO courseEventCreateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseEventCreationUseCase.create(courseEventCreateRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetCourseEventResponseDTO>> getAllCourseEvent() {
        return ResponseEntity.ok(getAllCourseEventsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetCourseEventResponseDTO> getCourseEventById(Long courseEventId) {
        return ResponseEntity.ok(getCourseEventByIdUseCase.get(courseEventId));
    }

    @Override
    public ResponseEntity<Void> deleteCourseEventById(Long courseEventId) {
        deleteCourseEventUseCase.delete(courseEventId);
        return ResponseEntity.noContent().build();
    }
}
