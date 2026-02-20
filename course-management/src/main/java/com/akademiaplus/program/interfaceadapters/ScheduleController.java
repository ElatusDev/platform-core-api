/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.interfaceadapters;

import com.akademiaplus.program.usecases.DeleteScheduleUseCase;
import com.akademiaplus.program.usecases.GetAllSchedulesUseCase;
import com.akademiaplus.program.usecases.GetScheduleByIdUseCase;
import com.akademiaplus.program.usecases.ScheduleCreationUseCase;
import openapi.akademiaplus.domain.course.management.api.SchedulesApi;
import openapi.akademiaplus.domain.course.management.dto.GetScheduleResponseDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for schedule management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/course-management")
public class ScheduleController implements SchedulesApi {

    private final ScheduleCreationUseCase scheduleCreationUseCase;
    private final GetAllSchedulesUseCase getAllSchedulesUseCase;
    private final GetScheduleByIdUseCase getScheduleByIdUseCase;
    private final DeleteScheduleUseCase deleteScheduleUseCase;

    public ScheduleController(ScheduleCreationUseCase scheduleCreationUseCase,
                              GetAllSchedulesUseCase getAllSchedulesUseCase,
                              GetScheduleByIdUseCase getScheduleByIdUseCase,
                              DeleteScheduleUseCase deleteScheduleUseCase) {
        this.scheduleCreationUseCase = scheduleCreationUseCase;
        this.getAllSchedulesUseCase = getAllSchedulesUseCase;
        this.getScheduleByIdUseCase = getScheduleByIdUseCase;
        this.deleteScheduleUseCase = deleteScheduleUseCase;
    }

    @Override
    public ResponseEntity<ScheduleCreationResponseDTO> createSchedule(
            ScheduleCreationRequestDTO scheduleCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(scheduleCreationUseCase.create(scheduleCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetScheduleResponseDTO>> getSchedules() {
        return ResponseEntity.ok(getAllSchedulesUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetScheduleResponseDTO> getScheduleById(Long scheduleId) {
        return ResponseEntity.ok(getScheduleByIdUseCase.get(scheduleId));
    }

    @Override
    public ResponseEntity<Void> deleteScheduleById(Long scheduleId) {
        deleteScheduleUseCase.delete(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
