/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import com.akademiaplus.util.base.DataFactory;
import lombok.RequiredArgsConstructor;
import openapi.akademiaplus.domain.billing.dto.CompensationCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link CompensationCreationRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class CompensationFactory implements DataFactory<CompensationCreationRequestDTO> {

    private final CompensationDataGenerator generator;

    @Override
    public List<CompensationCreationRequestDTO> generate(int count) {
        List<CompensationCreationRequestDTO> compensations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            compensations.add(createCompensation());
        }
        return compensations;
    }

    private CompensationCreationRequestDTO createCompensation() {
        CompensationCreationRequestDTO dto = new CompensationCreationRequestDTO();
        dto.setCompensationType(generator.compensationType());
        dto.setAmount(generator.amount());
        return dto;
    }
}
