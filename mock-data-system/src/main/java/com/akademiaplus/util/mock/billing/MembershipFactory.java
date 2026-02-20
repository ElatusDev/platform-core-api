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
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating {@link MembershipCreationRequestDTO} instances with fake data.
 */
@Component
@RequiredArgsConstructor
public class MembershipFactory implements DataFactory<MembershipCreationRequestDTO> {

    private final MembershipDataGenerator generator;

    @Override
    public List<MembershipCreationRequestDTO> generate(int count) {
        List<MembershipCreationRequestDTO> memberships = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            memberships.add(createMembership());
        }
        return memberships;
    }

    private MembershipCreationRequestDTO createMembership() {
        MembershipCreationRequestDTO dto = new MembershipCreationRequestDTO();
        dto.setMembershipType(generator.membershipType());
        dto.setFee(generator.fee());
        dto.setDescription(generator.description());
        return dto;
    }
}
