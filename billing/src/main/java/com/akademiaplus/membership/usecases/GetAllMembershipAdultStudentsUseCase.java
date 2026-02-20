/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Use case for retrieving all membership adult students in the current tenant.
 */
@Service
public class GetAllMembershipAdultStudentsUseCase {

    private final MembershipAdultStudentRepository membershipAdultStudentRepository;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetAllMembershipAdultStudentsUseCase with the required dependencies.
     *
     * @param membershipAdultStudentRepository the repository for membership adult student data access
     * @param modelMapper                      the mapper for entity-to-DTO conversion
     */
    public GetAllMembershipAdultStudentsUseCase(MembershipAdultStudentRepository membershipAdultStudentRepository, ModelMapper modelMapper) {
        this.membershipAdultStudentRepository = membershipAdultStudentRepository;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves all membership adult students for the current tenant context.
     *
     * @return a list of membership adult student response DTOs
     */
    public List<GetMembershipAdultStudentResponseDTO> getAll() {
        return membershipAdultStudentRepository.findAll().stream()
                .map(dataModel -> modelMapper.map(dataModel, GetMembershipAdultStudentResponseDTO.class))
                .toList();
    }
}
