/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import openapi.akademiaplus.domain.my.dto.MyMembershipDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves memberships for the authenticated adult student.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyMembershipsUseCase {

    private final UserContextHolder userContextHolder;
    private final MembershipAdultStudentRepository membershipAdultStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder                 the user context holder
     * @param membershipAdultStudentRepository  the membership-student repository
     */
    public GetMyMembershipsUseCase(UserContextHolder userContextHolder,
                                    MembershipAdultStudentRepository membershipAdultStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.membershipAdultStudentRepository = membershipAdultStudentRepository;
    }

    /**
     * Retrieves all memberships for the authenticated student.
     *
     * @return list of membership DTOs
     */
    @Transactional(readOnly = true)
    public List<MyMembershipDTO> execute() {
        Long profileId = userContextHolder.requireProfileId();

        return membershipAdultStudentRepository.findByAdultStudentId(profileId).stream()
                .map(m -> {
                    MyMembershipDTO dto = new MyMembershipDTO();
                    dto.setMembershipAdultStudentId(m.getMembershipAdultStudentId());
                    if (m.getMembership() != null) {
                        dto.setMembershipType(m.getMembership().getMembershipType());
                        dto.setFee(m.getMembership().getFee() != null ? m.getMembership().getFee().doubleValue() : null);
                    }
                    dto.setStartDate(m.getStartDate());
                    dto.setDueDate(m.getDueDate());
                    dto.setCourseId(m.getCourseId());
                    if (m.getCourse() != null) {
                        dto.setCourseName(m.getCourse().getCourseName());
                    }
                    return dto;
                })
                .toList();
    }
}
