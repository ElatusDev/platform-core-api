/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.course;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link CourseCreationRequestDTO} instances with fake data.
 *
 * <p>No {@code CourseCreationUseCase} exists in the domain layer, so the
 * configuration class provides a direct entity-mapping transformer instead.</p>
 */
@Component
@RequiredArgsConstructor
public class CourseFactory implements DataFactory<CourseCreationRequestDTO> {

    private final CourseDataGenerator generator;

    @Override
    public List<CourseCreationRequestDTO> generate(int count) {
        List<CourseCreationRequestDTO> courses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            courses.add(createCourse());
        }
        return courses;
    }

    private CourseCreationRequestDTO createCourse() {
        CourseCreationRequestDTO dto = new CourseCreationRequestDTO();
        dto.setName(generator.courseName());
        dto.setDescription(generator.courseDescription());
        dto.setMaxCapacity(generator.maxCapacity());
        return dto;
    }
}
