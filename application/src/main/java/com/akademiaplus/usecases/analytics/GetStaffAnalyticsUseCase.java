/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.RoleCountDTO;
import openapi.akademiaplus.domain.analytics.dto.StaffAnalyticsDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates staff analytics for the tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetStaffAnalyticsUseCase {

    public static final String CACHE_NAME = "analytics:staff";

    private final AnalyticsQueryService queryService;

    public GetStaffAnalyticsUseCase(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId")
    public StaffAnalyticsDTO get(Long tenantId) {
        StaffAnalyticsDTO dto = new StaffAnalyticsDTO();
        dto.setEmployeeCount((int) queryService.countEmployees(tenantId));
        dto.setTutorCount((int) queryService.countTutors(tenantId));
        dto.setCollaboratorCount((int) queryService.countCollaborators(tenantId));

        List<RoleCountDTO> distribution = queryService.getEmployeeDistributionByRole(tenantId).stream()
                .map(row -> {
                    RoleCountDTO rc = new RoleCountDTO();
                    rc.setRole((String) row[0]);
                    rc.setCount(((Number) row[1]).intValue());
                    return rc;
                })
                .toList();
        dto.setDistributionByRole(distribution);
        return dto;
    }
}
