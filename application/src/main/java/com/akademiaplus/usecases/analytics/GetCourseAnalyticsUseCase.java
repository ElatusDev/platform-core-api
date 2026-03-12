/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.CourseAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.CourseCapacityDTO;
import openapi.akademiaplus.domain.analytics.dto.CourseEnrollmentDTO;
import openapi.akademiaplus.domain.analytics.dto.DayScheduleDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates course analytics for the tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetCourseAnalyticsUseCase {

    public static final String CACHE_NAME = "analytics:courses";

    private final AnalyticsQueryService queryService;

    public GetCourseAnalyticsUseCase(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId")
    public CourseAnalyticsDTO get(Long tenantId) {
        CourseAnalyticsDTO dto = new CourseAnalyticsDTO();

        dto.setEnrollmentByCourse(queryService.getCourseEnrollments(tenantId).stream()
                .map(row -> {
                    CourseEnrollmentDTO ce = new CourseEnrollmentDTO();
                    ce.setCourseId(((Number) row[0]).longValue());
                    ce.setCourseName((String) row[1]);
                    ce.setEnrolledCount(((Number) row[2]).intValue());
                    return ce;
                })
                .toList());

        dto.setCapacityUtilization(queryService.getCourseCapacity(tenantId).stream()
                .map(row -> {
                    CourseCapacityDTO cc = new CourseCapacityDTO();
                    cc.setCourseId(((Number) row[0]).longValue());
                    cc.setCourseName((String) row[1]);
                    int capacity = ((Number) row[2]).intValue();
                    int enrolled = ((Number) row[3]).intValue();
                    cc.setCapacity(capacity);
                    cc.setEnrolled(enrolled);
                    cc.setUtilizationPercent(capacity > 0 ? (enrolled * 100.0 / capacity) : 0.0);
                    return cc;
                })
                .toList());

        dto.setScheduleByDayOfWeek(queryService.getScheduleByDayOfWeek(tenantId).stream()
                .map(row -> {
                    DayScheduleDTO ds = new DayScheduleDTO();
                    ds.setDayOfWeek((String) row[0]);
                    ds.setClassCount(((Number) row[1]).intValue());
                    return ds;
                })
                .toList());

        return dto;
    }
}
