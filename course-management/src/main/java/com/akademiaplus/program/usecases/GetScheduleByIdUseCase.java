/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a schedule by its identifier within the current tenant.
 */
@Service
public class GetScheduleByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final ScheduleRepository scheduleRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetScheduleByIdUseCase with the required dependencies.
     *
     * @param scheduleRepository  the repository for schedule data access
     * @param tenantContextHolder the holder for the current tenant context
     * @param modelMapper         the mapper for entity-to-DTO conversion
     */
    public GetScheduleByIdUseCase(ScheduleRepository scheduleRepository,
                                  TenantContextHolder tenantContextHolder,
                                  ModelMapper modelMapper) {
        this.scheduleRepository = scheduleRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a schedule by its identifier within the current tenant context.
     *
     * @param scheduleId the unique identifier of the schedule
     * @return the schedule response DTO
     * @throws IllegalArgumentException if tenant context is not available
     * @throws EntityNotFoundException  if no schedule is found with the given identifier
     */
    public GetScheduleResponseDTO get(Long scheduleId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<ScheduleDataModel> queryResult = scheduleRepository.findById(
                new ScheduleDataModel.ScheduleCompositeId(tenantId, scheduleId));
        if (queryResult.isPresent()) {
            ScheduleDataModel found = queryResult.get();
            return modelMapper.map(found, GetScheduleResponseDTO.class);
        } else {
            throw new EntityNotFoundException(EntityType.SCHEDULE, String.valueOf(scheduleId));
        }
    }
}
