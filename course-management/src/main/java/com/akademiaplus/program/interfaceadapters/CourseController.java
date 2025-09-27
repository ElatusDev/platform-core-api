package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.program.usecases.CreateCourseUseCase;
import openapi.akademiaplus.domain.course_management.api.CoursesApi;
import openapi.akademiaplus.domain.course_management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course_management.dto.CourseCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/course-management")
public class CourseController implements CoursesApi {
    private final CreateCourseUseCase createCourseUseCase;

    public CourseController(CreateCourseUseCase createCourseUseCase) {
        this.createCourseUseCase = createCourseUseCase;
    }

    @Override
    public ResponseEntity<CourseCreationResponseDTO> createCourse(CourseCreationRequestDTO courseCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(createCourseUseCase.create(courseCreationRequestDTO));
    }
}
