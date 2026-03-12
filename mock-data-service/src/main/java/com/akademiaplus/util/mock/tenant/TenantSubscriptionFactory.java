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
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link SubscriptionCreateRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class TenantSubscriptionFactory implements DataFactory<SubscriptionCreateRequestDTO> {

    private final TenantSubscriptionDataGenerator generator;

    @Override
    public List<SubscriptionCreateRequestDTO> generate(int count) {
        List<SubscriptionCreateRequestDTO> subscriptions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            subscriptions.add(createSubscription());
        }
        return subscriptions;
    }

    private SubscriptionCreateRequestDTO createSubscription() {
        SubscriptionCreateRequestDTO dto = new SubscriptionCreateRequestDTO();
        dto.setType(generator.type());
        dto.setMaxUsers(generator.maxUsers());
        dto.setBillingDate(generator.billingDate());
        dto.setRatePerStudent(generator.ratePerStudent());
        return dto;
    }
}
