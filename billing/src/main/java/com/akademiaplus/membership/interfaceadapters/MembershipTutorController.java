/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.membership.usecases.DeleteMembershipTutorUseCase;
import com.akademiaplus.membership.usecases.GetAllMembershipTutorsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipTutorByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipTutorCreationUseCase;
import openapi.akademiaplus.domain.billing.api.MembershipTutorsApi;
import openapi.akademiaplus.domain.billing.dto.GetMembershipTutorResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for membership-tutor association operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class MembershipTutorController implements MembershipTutorsApi {

    private final MembershipTutorCreationUseCase membershipTutorCreationUseCase;
    private final GetAllMembershipTutorsUseCase getAllMembershipTutorsUseCase;
    private final GetMembershipTutorByIdUseCase getMembershipTutorByIdUseCase;
    private final DeleteMembershipTutorUseCase deleteMembershipTutorUseCase;

    public MembershipTutorController(
            MembershipTutorCreationUseCase membershipTutorCreationUseCase,
            GetAllMembershipTutorsUseCase getAllMembershipTutorsUseCase,
            GetMembershipTutorByIdUseCase getMembershipTutorByIdUseCase,
            DeleteMembershipTutorUseCase deleteMembershipTutorUseCase) {
        this.membershipTutorCreationUseCase = membershipTutorCreationUseCase;
        this.getAllMembershipTutorsUseCase = getAllMembershipTutorsUseCase;
        this.getMembershipTutorByIdUseCase = getMembershipTutorByIdUseCase;
        this.deleteMembershipTutorUseCase = deleteMembershipTutorUseCase;
    }

    @Override
    public ResponseEntity<MembershipTutorCreationResponseDTO> createMembershipTutor(
            MembershipTutorCreationRequestDTO membershipTutorCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membershipTutorCreationUseCase.create(membershipTutorCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetMembershipTutorResponseDTO>> getMembershipTutors() {
        return ResponseEntity.ok(getAllMembershipTutorsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetMembershipTutorResponseDTO> getMembershipTutorById(
            Long membershipTutorId) {
        return ResponseEntity.ok(getMembershipTutorByIdUseCase.get(membershipTutorId));
    }

    @Override
    public ResponseEntity<Void> deleteMembershipTutor(Long membershipTutorId) {
        deleteMembershipTutorUseCase.delete(membershipTutorId);
        return ResponseEntity.noContent().build();
    }
}
