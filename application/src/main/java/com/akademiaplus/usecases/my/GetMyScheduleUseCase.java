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
import openapi.akademiaplus.domain.my.dto.MyScheduleDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves the class schedule for the authenticated student's enrolled courses.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyScheduleUseCase {

    private final UserContextHolder userContextHolder;
    private final MembershipAdultStudentRepository membershipAdultStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder                 the user context holder
     * @param membershipAdultStudentRepository  the membership-student repository
     */
    public GetMyScheduleUseCase(UserContextHolder userContextHolder,
                                 MembershipAdultStudentRepository membershipAdultStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.membershipAdultStudentRepository = membershipAdultStudentRepository;
    }

    /**
     * Retrieves the schedule entries for the authenticated student's courses.
     *
     * @return list of schedule DTOs
     */
    @Transactional(readOnly = true)
    public List<MyScheduleDTO> execute() {
        Long profileId = userContextHolder.requireProfileId();

        return membershipAdultStudentRepository.findByAdultStudentId(profileId).stream()
                .filter(m -> m.getCourse() != null)
                .flatMap(m -> m.getCourse().getSchedules().stream()
                        .map(s -> {
                            MyScheduleDTO dto = new MyScheduleDTO();
                            dto.setScheduleId(s.getScheduleId());
                            dto.setCourseId(m.getCourse().getCourseId());
                            dto.setCourseName(m.getCourse().getCourseName());
                            dto.setScheduleDay(s.getScheduleDay());
                            dto.setStartTime(s.getStartTime() != null ? s.getStartTime().toString() : null);
                            dto.setEndTime(s.getEndTime() != null ? s.getEndTime().toString() : null);
                            return dto;
                        }))
                .toList();
    }
}
