/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all schedules in the current tenant.
 */
@Service
public class GetAllSchedulesUseCase {

    private final ScheduleRepository scheduleRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllSchedulesUseCase with the required dependencies.
     *
     * @param scheduleRepository the repository for schedule data access
     * @param modelMapper        the mapper for entity-to-DTO conversion
     */
    public GetAllSchedulesUseCase(ScheduleRepository scheduleRepository, ModelMapper modelMapper) {
        this.scheduleRepository = scheduleRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all schedules for the current tenant context.
     *
     * @return a list of schedule response DTOs
     */
    public List<GetScheduleResponseDTO> getAll() {
        return scheduleRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetScheduleResponseDTO.class))
                .toList();
    }
}
