/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all memberships in the current tenant.
 */
@Service
public class GetAllMembershipsUseCase {

    private final MembershipRepository membershipRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllMembershipsUseCase with the required dependencies.
     *
     * @param membershipRepository the repository for membership data access
     * @param modelMapper          the mapper for entity-to-DTO conversion
     */
    public GetAllMembershipsUseCase(MembershipRepository membershipRepository, ModelMapper modelMapper) {
        this.membershipRepository = membershipRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all memberships for the current tenant context.
     *
     * @return a list of membership response DTOs
     */
    public List<GetMembershipResponseDTO> getAll() {
        return membershipRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetMembershipResponseDTO.class))
                .toList();
    }
}
