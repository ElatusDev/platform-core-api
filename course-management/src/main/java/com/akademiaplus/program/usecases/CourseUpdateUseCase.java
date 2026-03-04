/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.CourseUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handles course updates including simple field mapping, collaborator
 * validation, and schedule reassignment.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class CourseUpdateUseCase {

    public static final String MAP_NAME = "courseUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Course updated successfully";

    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final CourseValidator courseValidator;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates an existing course with the provided request data.
     *
     * @param courseId the ID of the course to update
     * @param dto     the update request containing new field values
     * @return response containing the course ID and success message
     * @throws EntityNotFoundException         if the course, collaborators, or schedules do not exist
     * @throws ScheduleNotAvailableException   if a schedule is already assigned to another course
     */
    @Transactional
    public CourseUpdateResponseDTO update(Long courseId, CourseUpdateRequestDTO dto) {

        // 1. Load existing entity by composite key
        Long tenantId = tenantContextHolder.requireTenantId();
        CourseDataModel existing = courseRepository
                .findById(new CourseDataModel.CourseCompositeId(tenantId, courseId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COURSE, String.valueOf(courseId)));

        // 2. Map simple fields via named TypeMap (name, description, maxCapacity)
        modelMapper.map(dto, existing, MAP_NAME);

        // 3. Validate and update collaborators
        List<CollaboratorDataModel> collaborators =
                courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
        existing.setAvailableCollaborators(collaborators);

        // 4. Handle schedule reassignment
        reassignSchedules(existing, dto.getTimeTableIds(), tenantId, courseId);

        // 5. Save and return manually-constructed response
        courseRepository.saveAndFlush(existing);

        CourseUpdateResponseDTO response = new CourseUpdateResponseDTO();
        response.setCourseId(courseId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }

    private void reassignSchedules(CourseDataModel existing, List<Long> newScheduleIds,
                                   Long tenantId, Long courseId) {
        // Unlink schedules no longer in the new list
        List<ScheduleDataModel> currentSchedules = existing.getSchedules();
        if (currentSchedules != null) {
            Set<Long> newIdSet = new HashSet<>(newScheduleIds);
            List<ScheduleDataModel> toUnlink = currentSchedules.stream()
                    .filter(s -> !newIdSet.contains(s.getScheduleId()))
                    .toList();
            toUnlink.forEach(s -> s.setCourseId(null));
            scheduleRepository.saveAll(toUnlink);
        }

        // Validate new schedules exist
        List<ScheduleDataModel.ScheduleCompositeId> compositeIds = newScheduleIds.stream()
                .map(id -> new ScheduleDataModel.ScheduleCompositeId(tenantId, id))
                .toList();
        List<ScheduleDataModel> foundSchedules = scheduleRepository.findAllById(compositeIds);

        if (foundSchedules.size() != newScheduleIds.size()) {
            Set<Long> foundIds = foundSchedules.stream()
                    .map(ScheduleDataModel::getScheduleId)
                    .collect(Collectors.toSet());
            String missingIds = newScheduleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new EntityNotFoundException(EntityType.SCHEDULE, missingIds);
        }

        // Validate no schedule is assigned to another course
        List<ScheduleDataModel> conflicting = foundSchedules.stream()
                .filter(s -> s.getCourseId() != null && !s.getCourseId().equals(courseId))
                .toList();
        if (!conflicting.isEmpty()) {
            String conflictInfo = conflicting.stream()
                    .map(s -> String.valueOf(s.getScheduleId()))
                    .collect(Collectors.joining(", "));
            throw new ScheduleNotAvailableException(conflictInfo);
        }

        // Assign all to this course
        foundSchedules.forEach(s -> s.setCourseId(courseId));
        scheduleRepository.saveAll(foundSchedules);
    }
}
