/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import openapi.akademiaplus.domain.my.dto.MyCourseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves courses that the authenticated collaborator is available to teach.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyCollaboratorCoursesUseCase {

    /** Error message when profile type is not COLLABORATOR. */
    public static final String ERROR_NOT_COLLABORATOR = "Only collaborators can access their available courses";

    private final UserContextHolder userContextHolder;
    private final CourseRepository courseRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder the user context holder
     * @param courseRepository   the course repository
     */
    public GetMyCollaboratorCoursesUseCase(UserContextHolder userContextHolder,
                                            CourseRepository courseRepository) {
        this.userContextHolder = userContextHolder;
        this.courseRepository = courseRepository;
    }

    /**
     * Returns all courses the authenticated collaborator is available to teach.
     *
     * @return list of course DTOs
     * @throws IllegalStateException if the profile type is not COLLABORATOR
     */
    @Transactional(readOnly = true)
    public List<MyCourseDTO> execute() {
        String profileType = userContextHolder.requireProfileType();
        if (!JwtTokenProvider.PROFILE_TYPE_COLLABORATOR.equals(profileType)) {
            throw new IllegalStateException(ERROR_NOT_COLLABORATOR);
        }
        Long profileId = userContextHolder.requireProfileId();

        List<CourseDataModel> courses = courseRepository.findByAvailableCollaboratorId(profileId);
        return courses.stream().map(this::mapToDto).toList();
    }

    private MyCourseDTO mapToDto(CourseDataModel course) {
        MyCourseDTO dto = new MyCourseDTO();
        dto.setCourseId(course.getCourseId());
        dto.setCourseName(course.getCourseName());
        dto.setCourseDescription(course.getCourseDescription());
        dto.setMaxCapacity(course.getMaxCapacity());
        return dto;
    }
}
