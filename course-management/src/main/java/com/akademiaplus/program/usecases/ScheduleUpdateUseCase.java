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
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.course.management.dto.ScheduleUpdateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleUpdateResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles schedule updates by loading the existing entity, applying field
 * changes via a named TypeMap, validating the course FK, and persisting.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class ScheduleUpdateUseCase {

    public static final String MAP_NAME = "scheduleUpdateMap";
    public static final String UPDATE_SUCCESS_MESSAGE = "Schedule updated successfully";

    private final ScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Updates an existing schedule with the provided request data.
     *
     * @param scheduleId the ID of the schedule to update
     * @param dto        the update request containing new field values
     * @return response containing the schedule ID and success message
     * @throws EntityNotFoundException if the schedule or referenced course does not exist
     */
    @Transactional
    public ScheduleUpdateResponseDTO update(Long scheduleId, ScheduleUpdateRequestDTO dto) {

        // 1. Load existing entity by composite key
        Long tenantId = tenantContextHolder.requireTenantId();
        ScheduleDataModel existing = scheduleRepository
                .findById(new ScheduleDataModel.ScheduleCompositeId(tenantId, scheduleId))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.SCHEDULE, String.valueOf(scheduleId)));

        // 2. Map simple fields via named TypeMap (skips ID + course relationship)
        modelMapper.map(dto, existing, MAP_NAME);

        // 3. Validate courseId FK references an existing course
        courseRepository.findById(new CourseDataModel.CourseCompositeId(tenantId, dto.getCourseId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        EntityType.COURSE, String.valueOf(dto.getCourseId())));

        // 4. Save and return manually-constructed response
        scheduleRepository.saveAndFlush(existing);

        ScheduleUpdateResponseDTO response = new ScheduleUpdateResponseDTO();
        response.setScheduleId(scheduleId);
        response.setMessage(UPDATE_SUCCESS_MESSAGE);
        return response;
    }
}
