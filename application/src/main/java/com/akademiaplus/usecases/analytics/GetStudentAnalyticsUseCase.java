/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.MonthlyCountDTO;
import openapi.akademiaplus.domain.analytics.dto.StudentAnalyticsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates student analytics for the tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetStudentAnalyticsUseCase {

    public static final String CACHE_NAME = "analytics:students";

    private final AnalyticsQueryService queryService;

    public GetStudentAnalyticsUseCase(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId")
    public StudentAnalyticsDTO get(Long tenantId) {
        StudentAnalyticsDTO dto = new StudentAnalyticsDTO();
        dto.setAdultCount((int) queryService.countAdultStudents(tenantId));
        dto.setMinorCount((int) queryService.countMinorStudents(tenantId));
        dto.setNewThisMonth((int) queryService.countNewStudentsThisMonth(tenantId));

        List<MonthlyCountDTO> trend = queryService.getEnrollmentTrend(tenantId).stream()
                .map(row -> {
                    MonthlyCountDTO mc = new MonthlyCountDTO();
                    mc.setMonth((String) row[0]);
                    mc.setCount(((Number) row[1]).intValue());
                    return mc;
                })
                .toList();
        dto.setEnrollmentTrend(trend);
        return dto;
    }
}
