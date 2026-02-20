/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.exception.MembershipNotFoundException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Use case for retrieving a membership by its identifier within the current tenant.
 */
@Service
public class GetMembershipByIdUseCase {

    /** Error message when tenant context is not available. */
    public static final String ERROR_TENANT_CONTEXT_REQUIRED = "Tenant context is required";

    private final MembershipRepository membershipRepository;
    private final TenantContextHolder tenantContextHolder;
    private final ModelMapper modelMapper;

    /**
     * Constructs a new GetMembershipByIdUseCase with the required dependencies.
     *
     * @param membershipRepository the repository for membership data access
     * @param tenantContextHolder  the holder for the current tenant context
     * @param modelMapper          the mapper for entity-to-DTO conversion
     */
    public GetMembershipByIdUseCase(MembershipRepository membershipRepository,
                                    TenantContextHolder tenantContextHolder,
                                    ModelMapper modelMapper) {
        this.membershipRepository = membershipRepository;
        this.tenantContextHolder = tenantContextHolder;
        this.modelMapper = modelMapper;
    }

    /**
     * Retrieves a membership by its identifier within the current tenant context.
     *
     * @param membershipId the unique identifier of the membership
     * @return the membership response DTO
     * @throws IllegalArgumentException      if tenant context is not available
     * @throws MembershipNotFoundException   if no membership is found with the given identifier
     */
    public GetMembershipResponseDTO get(Long membershipId) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException(ERROR_TENANT_CONTEXT_REQUIRED));
        Optional<MembershipDataModel> queryResult = membershipRepository.findById(
                new MembershipDataModel.MembershipCompositeId(tenantId, membershipId));
        if (queryResult.isPresent()) {
            MembershipDataModel found = queryResult.get();
            return modelMapper.map(found, GetMembershipResponseDTO.class);
        } else {
            throw new MembershipNotFoundException(String.valueOf(membershipId));
        }
    }
}
