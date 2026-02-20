/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link BillingCycleCreateRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class TenantBillingCycleFactory implements DataFactory<BillingCycleCreateRequestDTO> {

    private final TenantBillingCycleDataGenerator generator;

    @Override
    public List<BillingCycleCreateRequestDTO> generate(int count) {
        List<BillingCycleCreateRequestDTO> cycles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cycles.add(createBillingCycle());
        }
        return cycles;
    }

    private BillingCycleCreateRequestDTO createBillingCycle() {
        BillingCycleCreateRequestDTO dto = new BillingCycleCreateRequestDTO();
        dto.setBillingMonth(generator.billingMonth());
        dto.setCalculationDate(generator.calculationDate());
        dto.setUserCount(generator.userCount());
        dto.setTotalAmount(generator.totalAmount());
        dto.setNotes(generator.notes());
        return dto;
    }
}
