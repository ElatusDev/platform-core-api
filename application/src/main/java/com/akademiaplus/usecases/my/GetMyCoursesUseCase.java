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
import openapi.akademiaplus.domain.my.dto.MyCourseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves courses the authenticated adult student is enrolled in.
 * Courses are resolved through the membership association.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyCoursesUseCase {

    private final UserContextHolder userContextHolder;
    private final MembershipAdultStudentRepository membershipAdultStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder                 the user context holder
     * @param membershipAdultStudentRepository  the membership-student repository
     */
    public GetMyCoursesUseCase(UserContextHolder userContextHolder,
                                MembershipAdultStudentRepository membershipAdultStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.membershipAdultStudentRepository = membershipAdultStudentRepository;
    }

    /**
     * Retrieves the courses for the authenticated student.
     *
     * @return list of course DTOs
     */
    @Transactional(readOnly = true)
    public List<MyCourseDTO> execute() {
        Long profileId = userContextHolder.requireProfileId();

        return membershipAdultStudentRepository.findByAdultStudentId(profileId).stream()
                .filter(m -> m.getCourse() != null)
                .map(m -> {
                    MyCourseDTO dto = new MyCourseDTO();
                    dto.setCourseId(m.getCourse().getCourseId());
                    dto.setCourseName(m.getCourse().getCourseName());
                    dto.setCourseDescription(m.getCourse().getCourseDescription());
                    dto.setMaxCapacity(m.getCourse().getMaxCapacity());
                    return dto;
                })
                .distinct()
                .toList();
    }
}
