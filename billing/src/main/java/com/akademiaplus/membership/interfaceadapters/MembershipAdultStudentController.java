/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.membership.usecases.GetAllMembershipAdultStudentsUseCase;
import com.akademiaplus.membership.usecases.GetMembershipAdultStudentByIdUseCase;
import com.akademiaplus.membership.usecases.MembershipAdultStudentCreationUseCase;
import openapi.akademiaplus.domain.billing.api.MembershipAdultStudentsApi;
import openapi.akademiaplus.domain.billing.dto.GetMembershipAdultStudentResponseDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for membership-adult-student association operations.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1/billing")
public class MembershipAdultStudentController implements MembershipAdultStudentsApi {

    private final MembershipAdultStudentCreationUseCase membershipAdultStudentCreationUseCase;
    private final GetAllMembershipAdultStudentsUseCase getAllMembershipAdultStudentsUseCase;
    private final GetMembershipAdultStudentByIdUseCase getMembershipAdultStudentByIdUseCase;

    public MembershipAdultStudentController(
            MembershipAdultStudentCreationUseCase membershipAdultStudentCreationUseCase,
            GetAllMembershipAdultStudentsUseCase getAllMembershipAdultStudentsUseCase,
            GetMembershipAdultStudentByIdUseCase getMembershipAdultStudentByIdUseCase) {
        this.membershipAdultStudentCreationUseCase = membershipAdultStudentCreationUseCase;
        this.getAllMembershipAdultStudentsUseCase = getAllMembershipAdultStudentsUseCase;
        this.getMembershipAdultStudentByIdUseCase = getMembershipAdultStudentByIdUseCase;
    }

    @Override
    public ResponseEntity<MembershipAdultStudentCreationResponseDTO> createMembershipAdultStudent(
            MembershipAdultStudentCreationRequestDTO membershipAdultStudentCreationRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(membershipAdultStudentCreationUseCase.create(membershipAdultStudentCreationRequestDTO));
    }

    @Override
    public ResponseEntity<List<GetMembershipAdultStudentResponseDTO>> getMembershipAdultStudents() {
        return ResponseEntity.ok(getAllMembershipAdultStudentsUseCase.getAll());
    }

    @Override
    public ResponseEntity<GetMembershipAdultStudentResponseDTO> getMembershipAdultStudentById(
            Long membershipAdultStudentId) {
        return ResponseEntity.ok(getMembershipAdultStudentByIdUseCase.get(membershipAdultStudentId));
    }
}
