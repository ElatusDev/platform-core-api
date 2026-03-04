/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.CourseEventUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles course event updates including field mapping, FK validation
 * for course, instructor, schedule, and attendee collection replacement.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class CourseEventUpdateUseCase {

    public static final String MAP_NAME = "courseEventUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Course event updated successfully";

    private final CourseEventRepository courseEventRepository;
    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final AdultStudentRepository adultStudentRepository;
    private final MinorStudentRepository minorStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates an existing course event with the provided request data.
     *
     * @param courseEventId the ID of the course event to update
     * @param dto          the update request containing new field values
     * @return response containing the course event ID and success message
     * @throws EntityNotFoundException if the course event or any referenced entity does not exist
     */
    @Transactional
    public CourseEventUpdateResponseDTO update(Long courseEventId, CourseEventUpdateRequestDTO dto) {

        // 1. Load existing entity by composite key
        Long tenantId = tenantContextHolder.requireTenantId();
        CourseEventDataModel existing = courseEventRepository
                .findById(new CourseEventDataModel.CourseEventCompositeId(tenantId, courseEventId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COURSE_EVENT, String.valueOf(courseEventId)));

        // 2. Map simple fields via named TypeMap (title → eventTitle, description → eventDescription)
        modelMapper.map(dto, existing, MAP_NAME);

        // 3. Validate and update FK references
        courseRepository.findById(new CourseDataModel.CourseCompositeId(tenantId, dto.getCourseId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COURSE, String.valueOf(dto.getCourseId())));
        existing.setCourseId(dto.getCourseId());

        collaboratorRepository.findById(
                        new CollaboratorDataModel.CollaboratorCompositeId(tenantId, dto.getInstructorId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COLLABORATOR, String.valueOf(dto.getInstructorId())));
        existing.setCollaboratorId(dto.getInstructorId());

        scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(tenantId, dto.getScheduleId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.SCHEDULE, String.valueOf(dto.getScheduleId())));
        existing.setScheduleId(dto.getScheduleId());

        // 4. Validate and replace attendee collections
        existing.setAdultAttendees(validateAdultAttendees(dto.getAdultAttendeeIds(), tenantId));
        existing.setMinorAttendees(validateMinorAttendees(dto.getMinorAttendeeIds(), tenantId));

        // 5. Save and return manually-constructed response
        courseEventRepository.saveAndFlush(existing);

        CourseEventUpdateResponseDTO response = new CourseEventUpdateResponseDTO();
        response.setCourseEventId(courseEventId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }

    private List<AdultStudentDataModel> validateAdultAttendees(List<Integer> attendeeIds, Long tenantId) {
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            return List.of();
        }
        List<AdultStudentDataModel.AdultStudentCompositeId> compositeIds = attendeeIds.stream()
                .map(id -> new AdultStudentDataModel.AdultStudentCompositeId(tenantId, id.longValue()))
                .toList();
        List<AdultStudentDataModel> found = adultStudentRepository.findAllById(compositeIds);
        if (found.size() != attendeeIds.size()) {
            Set<Long> foundIds = found.stream()
                    .map(AdultStudentDataModel::getAdultStudentId)
                    .collect(Collectors.toSet());
            String missingIds = attendeeIds.stream()
                    .filter(id -> !foundIds.contains(id.longValue()))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new EntityNotFoundException(EntityType.ADULT_STUDENT, missingIds);
        }
        return found;
    }

    private List<MinorStudentDataModel> validateMinorAttendees(List<Integer> attendeeIds, Long tenantId) {
        if (attendeeIds == null || attendeeIds.isEmpty()) {
            return List.of();
        }
        List<MinorStudentDataModel.MinorStudentCompositeId> compositeIds = attendeeIds.stream()
                .map(id -> new MinorStudentDataModel.MinorStudentCompositeId(tenantId, id.longValue()))
                .toList();
        List<MinorStudentDataModel> found = minorStudentRepository.findAllById(compositeIds);
        if (found.size() != attendeeIds.size()) {
            Set<Long> foundIds = found.stream()
                    .map(MinorStudentDataModel::getMinorStudentId)
                    .collect(Collectors.toSet());
            String missingIds = attendeeIds.stream()
                    .filter(id -> !foundIds.contains(id.longValue()))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new EntityNotFoundException(EntityType.MINOR_STUDENT, missingIds);
        }
        return found;
    }
}
