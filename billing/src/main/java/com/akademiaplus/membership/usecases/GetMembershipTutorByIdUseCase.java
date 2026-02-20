/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.exception.MembershipTutorNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a membership tutor by its identifier within the current tenant.
 */
@Service
public class GetMembershipTutorByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final MembershipTutorRepository membershipTutorRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetMembershipTutorByIdUseCase with the required dependencies.
     *
     * @param membershipTutorRepository the repository for membership tutor data access
     * @param tenantContextHolder       the holder for the current tenant context
     * @param modelMapper               the mapper for entity-to-DTO conversion
     */
    public GetMembershipTutorByIdUseCase(MembershipTutorRepository membershipTutorRepository,
                                         TenantContextHolder tenantContextHolder,
                                         ModelMapper modelMapper) {
        this.membershipTutorRepository = membershipTutorRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a membership tutor by its identifier within the current tenant context.
     *
     * @param membershipTutorId the unique identifier of the membership tutor
     * @return the membership tutor response DTO
     * @throws IllegalArgumentException              if tenant context is not available
     * @throws MembershipTutorNotFoundException      if no membership tutor is found with the given identifier
     */
    public GetMembershipTutorResponseDTO get(Long membershipTutorId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<MembershipTutorDataModel> queryResult = membershipTutorRepository.findById(
                new MembershipTutorDataModel.MembershipTutorCompositeId(tenantId, membershipTutorId));
        if (queryResult.isPresent()) {
            MembershipTutorDataModel found = queryResult.get();
            return modelMapper.map(found, GetMembershipTutorResponseDTO.class);
        } else {
            throw new MembershipTutorNotFoundException(String.valueOf(membershipTutorId));
        }
    }
}
