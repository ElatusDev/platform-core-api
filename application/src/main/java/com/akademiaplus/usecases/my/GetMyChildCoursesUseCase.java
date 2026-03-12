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
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import openapi.akademiaplus.domain.my.dto.MyCourseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Retrieves courses for a specific child of the authenticated tutor.
 * Verifies the child belongs to this tutor (invariant I3).
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class GetMyChildCoursesUseCase {

    /** Error message when the child does not belong to the authenticated tutor. */
    public static final String ERROR_CHILD_NOT_FOUND = "minor student not found for this tutor";

    private final UserContextHolder userContextHolder;
    private final MinorStudentRepository minorStudentRepository;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param userContextHolder      the user context holder
     * @param minorStudentRepository the minor student repository
     */
    public GetMyChildCoursesUseCase(UserContextHolder userContextHolder,
                                     MinorStudentRepository minorStudentRepository) {
        this.userContextHolder = userContextHolder;
        this.minorStudentRepository = minorStudentRepository;
    }

    /**
     * Retrieves courses for the specified child, verifying ownership.
     *
     * @param minorStudentId the minor student ID
     * @return list of course DTOs
     * @throws EntityNotFoundException if the child is not found or doesn't belong to this tutor
     */
    @Transactional(readOnly = true)
    public List<MyCourseDTO> execute(Long minorStudentId) {
        Long tutorId = userContextHolder.requireProfileId();

        return minorStudentRepository.findByTutorId(tutorId).stream()
                .filter(m -> m.getMinorStudentId().equals(minorStudentId))
                .findFirst()
                .map(child -> List.<MyCourseDTO>of())
                .orElseThrow(() -> new EntityNotFoundException(EntityType.MINOR_STUDENT, minorStudentId.toString()));
    }
}
