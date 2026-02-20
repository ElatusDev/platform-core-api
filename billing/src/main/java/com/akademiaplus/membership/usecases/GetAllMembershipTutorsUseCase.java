/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all membership tutors in the current tenant.
 */
@Service
public class GetAllMembershipTutorsUseCase {

    private final MembershipTutorRepository membershipTutorRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllMembershipTutorsUseCase with the required dependencies.
     *
     * @param membershipTutorRepository the repository for membership tutor data access
     * @param modelMapper               the mapper for entity-to-DTO conversion
     */
    public GetAllMembershipTutorsUseCase(MembershipTutorRepository membershipTutorRepository, ModelMapper modelMapper) {
        this.membershipTutorRepository = membershipTutorRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all membership tutors for the current tenant context.
     *
     * @return a list of membership tutor response DTOs
     */
    public List<GetMembershipTutorResponseDTO> getAll() {
        return membershipTutorRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetMembershipTutorResponseDTO.class))
                .toList();
    }
}
