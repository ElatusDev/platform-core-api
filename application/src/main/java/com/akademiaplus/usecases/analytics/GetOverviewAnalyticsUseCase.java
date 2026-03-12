/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.OverviewAnalyticsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates overview analytics for the tenant dashboard.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetOverviewAnalyticsUseCase {

    public static final String CACHE_NAME = "analytics:overview";

    private final AnalyticsQueryService queryService;

    public GetOverviewAnalyticsUseCase(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId")
    public OverviewAnalyticsDTO get(Long tenantId) {
        long adultCount = queryService.countAdultStudents(tenantId);
        long minorCount = queryService.countMinorStudents(tenantId);

        OverviewAnalyticsDTO dto = new OverviewAnalyticsDTO();
        dto.setTotalStudents((int) (adultCount + minorCount));
        dto.setRevenueMTD(queryService.getRevenueMTD(tenantId).doubleValue());
        dto.setCourseUtilization(queryService.getCourseUtilization(tenantId));
        dto.setStaffCount((int) (queryService.countEmployees(tenantId)
                + queryService.countTutors(tenantId)
                + queryService.countCollaborators(tenantId)));
        dto.setMembershipRenewalRate(queryService.getMembershipRenewalRate(tenantId));
        return dto;
    }
}
