/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import openapi.akademiaplus.domain.my.dto.MyClassDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves course events (classes) assigned to the authenticated collaborator.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyClassesUseCase {

    /** Error message when profile type is not COLLABORATOR. */
    public static final String ERROR_NOT_COLLABORATOR = "Only collaborators can access classes";

    private final UserContextHolder userContextHolder;
    private final CourseEventRepository courseEventRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder     the user context holder
     * @param courseEventRepository  the course event repository
     */
    public GetMyClassesUseCase(UserContextHolder userContextHolder,
                                CourseEventRepository courseEventRepository) {
        this.userContextHolder = userContextHolder;
        this.courseEventRepository = courseEventRepository;
    }

    /**
     * Returns all classes assigned to the authenticated collaborator.
     *
     * @return list of class DTOs
     * @throws IllegalStateException if the profile type is not COLLABORATOR
     */
    @Transactional(readOnly = true)
    public List<MyClassDTO> execute() {
        String profileType = userContextHolder.requireProfileType();
        if (!JwtTokenProvider.PROFILE_TYPE_COLLABORATOR.equals(profileType)) {
            throw new IllegalStateException(ERROR_NOT_COLLABORATOR);
        }
        Long profileId = userContextHolder.requireProfileId();

        List<CourseEventDataModel> events = courseEventRepository.findByCollaboratorId(profileId);
        return events.stream().map(this::mapToDto).toList();
    }

    private MyClassDTO mapToDto(CourseEventDataModel event) {
        MyClassDTO dto = new MyClassDTO();
        dto.setClassId(event.getCourseEventId());
        dto.setEventDate(event.getEventDate());
        dto.setEventTitle(event.getEventTitle());
        dto.setEventDescription(event.getEventDescription());
        dto.setCourseId(event.getCourseId());
        if (event.getCourse() != null) {
            dto.setCourseName(event.getCourse().getCourseName());
        }
        if (event.getSchedule() != null) {
            ScheduleDataModel schedule = event.getSchedule();
            dto.setScheduleDay(schedule.getScheduleDay());
            dto.setStartTime(schedule.getStartTime().toString());
            dto.setEndTime(schedule.getEndTime().toString());
        }
        return dto;
    }
}
