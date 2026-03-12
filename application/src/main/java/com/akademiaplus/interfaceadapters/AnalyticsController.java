/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.usecases.analytics.GetCourseAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetOverviewAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetRevenueAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetStaffAnalyticsUseCase;
import com.akademiaplus.usecases.analytics.GetStudentAnalyticsUseCase;
import openapi.akademiaplus.domain.analytics.api.AnalyticsApi;
import openapi.akademiaplus.domain.analytics.dto.CourseAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.OverviewAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.RevenueAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.StaffAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.StudentAnalyticsDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for analytics endpoints.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
public class AnalyticsController implements AnalyticsApi {

    private final GetOverviewAnalyticsUseCase overviewUseCase;
    private final GetStudentAnalyticsUseCase studentUseCase;
    private final GetCourseAnalyticsUseCase courseUseCase;
    private final GetStaffAnalyticsUseCase staffUseCase;
    private final GetRevenueAnalyticsUseCase revenueUseCase;
    private final TenantContextHolder tenantContextHolder;

    public AnalyticsController(GetOverviewAnalyticsUseCase overviewUseCase,
                               GetStudentAnalyticsUseCase studentUseCase,
                               GetCourseAnalyticsUseCase courseUseCase,
                               GetStaffAnalyticsUseCase staffUseCase,
                               GetRevenueAnalyticsUseCase revenueUseCase,
                               TenantContextHolder tenantContextHolder) {
        this.overviewUseCase = overviewUseCase;
        this.studentUseCase = studentUseCase;
        this.courseUseCase = courseUseCase;
        this.staffUseCase = staffUseCase;
        this.revenueUseCase = revenueUseCase;
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public ResponseEntity<OverviewAnalyticsDTO> getOverviewAnalytics() {
        Long tenantId = tenantContextHolder.requireTenantId();
        return ResponseEntity.ok(overviewUseCase.get(tenantId));
    }

    @Override
    public ResponseEntity<StudentAnalyticsDTO> getStudentAnalytics() {
        Long tenantId = tenantContextHolder.requireTenantId();
        return ResponseEntity.ok(studentUseCase.get(tenantId));
    }

    @Override
    public ResponseEntity<CourseAnalyticsDTO> getCourseAnalytics() {
        Long tenantId = tenantContextHolder.requireTenantId();
        return ResponseEntity.ok(courseUseCase.get(tenantId));
    }

    @Override
    public ResponseEntity<StaffAnalyticsDTO> getStaffAnalytics() {
        Long tenantId = tenantContextHolder.requireTenantId();
        return ResponseEntity.ok(staffUseCase.get(tenantId));
    }

    @Override
    public ResponseEntity<RevenueAnalyticsDTO> getRevenueAnalytics() {
        Long tenantId = tenantContextHolder.requireTenantId();
        return ResponseEntity.ok(revenueUseCase.get(tenantId));
    }
}
