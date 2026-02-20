/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.membership.usecases.GetAllMembershipsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipCreationUseCase;
import openapi.akademiaplus.domain.billing.api.MembershipsApi;
import openapi.akademiaplus.domain.billing.dto.GetMembershipResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for membership management operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class MembershipController implements MembershipsApi {

    private final MembershipCreationUseCase membershipCreationUseCase;
    private final GetAllMembershipsUseCase getAllMembershipsUseCase;
    private final GetMembershipByIdUseCase getMembershipByIdUseCase;

    public MembershipController(MembershipCreationUseCase membershipCreationUseCase,
                                GetAllMembershipsUseCase getAllMembershipsUseCase,
                                GetMembershipByIdUseCase getMembershipByIdUseCase) {
        this.membershipCreationUseCase = membershipCreationUseCase;
        this.getAllMembershipsUseCase = getAllMembershipsUseCase;
        this.getMembershipByIdUseCase = getMembershipByIdUseCase;
    }

    @Override
    public ResponseEntity<MembershipCreationResponseDTO> createMembership(
            MembershipCreationRequestDTO membershipCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membershipCreationUseCase.create(membershipCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetMembershipResponseDTO>> getMemberships() {
        return ResponseEntity.ok(getAllMembershipsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetMembershipResponseDTO> getMembershipById(Long membershipId) {
        return ResponseEntity.ok(getMembershipByIdUseCase.get(membershipId));
    }
}
