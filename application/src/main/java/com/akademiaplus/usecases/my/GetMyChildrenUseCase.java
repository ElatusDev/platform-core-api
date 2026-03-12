/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.my;

import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.internal.interfaceadapters.UserContextHolder;
import openapi.akademiaplus.domain.my.dto.MyChildDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves the minor students linked to the authenticated tutor.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyChildrenUseCase {

    private final UserContextHolder userContextHolder;
    private final MinorStudentRepository minorStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder      the user context holder
     * @param minorStudentRepository the minor student repository
     */
    public GetMyChildrenUseCase(UserContextHolder userContextHolder,
                                 MinorStudentRepository minorStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.minorStudentRepository = minorStudentRepository;
    }

    /**
     * Retrieves all minor students for the authenticated tutor.
     *
     * @return list of child DTOs
     */
    @Transactional(readOnly = true)
    public List<MyChildDTO> execute() {
        Long tutorId = userContextHolder.requireProfileId();

        return minorStudentRepository.findByTutorId(tutorId).stream()
                .map(m -> {
                    MyChildDTO dto = new MyChildDTO();
                    dto.setMinorStudentId(m.getMinorStudentId());
                    if (m.getPersonPII() != null) {
                        dto.setFirstName(m.getPersonPII().getFirstName());
                        dto.setLastName(m.getPersonPII().getLastName());
                    }
                    dto.setBirthDate(m.getBirthDate());
                    dto.setEntryDate(m.getEntryDate());
                    return dto;
                })
                .toList();
    }
}
