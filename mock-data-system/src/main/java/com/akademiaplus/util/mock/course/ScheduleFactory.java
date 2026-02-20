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
import lombok.Setter;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link ScheduleCreationRequestDTO} instances with fake data.
 *
 * <p>Requires {@link #setAvailableCourseIds(List)} to be called before
 * {@link #generate(int)}, so that each schedule is assigned to a valid,
 * previously-persisted course.</p>
 */
@Component
@RequiredArgsConstructor
public class ScheduleFactory implements DataFactory<ScheduleCreationRequestDTO> {

    private final ScheduleDataGenerator generator;

    @Setter
    private List<Long> availableCourseIds = List.of();

    @Override
    public List<ScheduleCreationRequestDTO> generate(int count) {
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException("availableCourseIds must be set before generating schedules");
        }
        List<ScheduleCreationRequestDTO> schedules = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Long courseId = availableCourseIds.get(i % availableCourseIds.size());
            schedules.add(createSchedule(courseId));
        }
        return schedules;
    }

    private ScheduleCreationRequestDTO createSchedule(Long courseId) {
        LocalTime startTime = generator.startTime();
        ScheduleCreationRequestDTO dto = new ScheduleCreationRequestDTO();
        dto.setScheduleDay(generator.scheduleDay());
        dto.setStartTime(startTime.format(DateTimeFormatter.ISO_LOCAL_TIME));
        dto.setEndTime(generator.endTime(startTime).format(DateTimeFormatter.ISO_LOCAL_TIME));
        dto.setCourseId(courseId);
        return dto;
    }
}
