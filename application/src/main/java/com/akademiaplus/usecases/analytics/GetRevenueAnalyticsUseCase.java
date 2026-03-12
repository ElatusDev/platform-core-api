/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.analytics;

import openapi.akademiaplus.domain.analytics.dto.MonthlyRevenueDTO;
import openapi.akademiaplus.domain.analytics.dto.RevenueAnalyticsDTO;
import openapi.akademiaplus.domain.analytics.dto.RevenueTypeDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregates revenue analytics for the tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetRevenueAnalyticsUseCase {

    public static final String CACHE_NAME = "analytics:revenue";

    private final AnalyticsQueryService queryService;

    public GetRevenueAnalyticsUseCase(AnalyticsQueryService queryService) {
        this.queryService = queryService;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = "#tenantId")
    public RevenueAnalyticsDTO get(Long tenantId) {
        RevenueAnalyticsDTO dto = new RevenueAnalyticsDTO();

        dto.setRevenueTrend(queryService.getRevenueTrend(tenantId).stream()
                .map(row -> {
                    MonthlyRevenueDTO mr = new MonthlyRevenueDTO();
                    mr.setMonth((String) row[0]);
                    Object amt = row[1];
                    mr.setAmount(amt instanceof BigDecimal bd ? bd.doubleValue() : ((Number) amt).doubleValue());
                    return mr;
                })
                .toList());

        dto.setBreakdownByType(queryService.getRevenueByType(tenantId).stream()
                .map(row -> {
                    RevenueTypeDTO rt = new RevenueTypeDTO();
                    rt.setType((String) row[0]);
                    Object amt = row[1];
                    rt.setAmount(amt instanceof BigDecimal bd ? bd.doubleValue() : ((Number) amt).doubleValue());
                    return rt;
                })
                .toList());

        dto.setOutstandingPayments(queryService.getOutstandingPayments(tenantId).doubleValue());
        return dto;
    }
}
