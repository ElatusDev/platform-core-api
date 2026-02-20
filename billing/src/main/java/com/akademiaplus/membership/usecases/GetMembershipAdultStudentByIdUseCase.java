/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.exception.MembershipAdultStudentNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a membership adult student by its identifier within the current tenant.
 */
@Service
public class GetMembershipAdultStudentByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final MembershipAdultStudentRepository membershipAdultStudentRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetMembershipAdultStudentByIdUseCase with the required dependencies.
     *
     * @param membershipAdultStudentRepository the repository for membership adult student data access
     * @param tenantContextHolder              the holder for the current tenant context
     * @param modelMapper                      the mapper for entity-to-DTO conversion
     */
    public GetMembershipAdultStudentByIdUseCase(MembershipAdultStudentRepository membershipAdultStudentRepository,
                                                TenantContextHolder tenantContextHolder,
                                                ModelMapper modelMapper) {
        this.membershipAdultStudentRepository = membershipAdultStudentRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a membership adult student by its identifier within the current tenant context.
     *
     * @param membershipAdultStudentId the unique identifier of the membership adult student
     * @return the membership adult student response DTO
     * @throws IllegalArgumentException                    if tenant context is not available
     * @throws MembershipAdultStudentNotFoundException     if no membership adult student is found with the given identifier
     */
    public GetMembershipAdultStudentResponseDTO get(Long membershipAdultStudentId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<MembershipAdultStudentDataModel> queryResult = membershipAdultStudentRepository.findById(
                new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(tenantId, membershipAdultStudentId));
        if (queryResult.isPresent()) {
            MembershipAdultStudentDataModel found = queryResult.get();
            return modelMapper.map(found, GetMembershipAdultStudentResponseDTO.class);
        } else {
            throw new MembershipAdultStudentNotFoundException(String.valueOf(membershipAdultStudentId));
        }
    }
}
