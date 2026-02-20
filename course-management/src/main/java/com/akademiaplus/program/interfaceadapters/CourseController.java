/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.program.usecases.CreateCourseUseCase;
import com.akademiaplus.program.usecases.DeleteCourseUseCase;
import com.akademiaplus.program.usecases.GetAllCoursesUseCase;
import com.akademiaplus.program.usecases.GetCourseByIdUseCase;
import openapi.akademiaplus.domain.course.management.api.CoursesApi;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationResponseDTO;
import openapi.akademiaplus.domain.course.management.dto.GetCourseResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for course management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/course-management")
public class CourseController implements CoursesApi {

    private final CreateCourseUseCase createCourseUseCase;
    private final GetAllCoursesUseCase getAllCoursesUseCase;
    private final GetCourseByIdUseCase getCourseByIdUseCase;
    private final DeleteCourseUseCase deleteCourseUseCase;

    public CourseController(CreateCourseUseCase createCourseUseCase,
                            GetAllCoursesUseCase getAllCoursesUseCase,
                            GetCourseByIdUseCase getCourseByIdUseCase,
                            DeleteCourseUseCase deleteCourseUseCase) {
        this.createCourseUseCase = createCourseUseCase;
        this.getAllCoursesUseCase = getAllCoursesUseCase;
        this.getCourseByIdUseCase = getCourseByIdUseCase;
        this.deleteCourseUseCase = deleteCourseUseCase;
    }

    @Override
    public ResponseEntity<CourseCreationResponseDTO> createCourse(
            CourseCreationRequestDTO courseCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createCourseUseCase.create(courseCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetCourseResponseDTO>> getCourses() {
        return ResponseEntity.ok(getAllCoursesUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetCourseResponseDTO> getCourseById(Long courseId) {
        return ResponseEntity.ok(getCourseByIdUseCase.get(courseId));
    }

    @Override
    public ResponseEntity<Void> deleteCourseById(Long courseId) {
        deleteCourseUseCase.delete(courseId);
        return ResponseEntity.noContent().build();
    }
}
